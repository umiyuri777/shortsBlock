package com.example.shortblocker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.shortblocker.action.BlockActionManager
import com.example.shortblocker.config.ConfigurationManager
import com.example.shortblocker.data.SettingsRepository
import com.example.shortblocker.detector.PlatformDetectorManager
import com.example.shortblocker.error.ErrorHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * AccessibilityService implementation for detecting and blocking short video content.
 * 
 * This service monitors accessibility events from target apps (YouTube, Instagram, TikTok)
 * and detects when users navigate to short video sections (Shorts, Reels, etc.).
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
@AndroidEntryPoint
class BlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BlockerAccessibilityService"
        
        // Target package names
        private val TARGET_PACKAGES = setOf(
            "com.google.android.youtube",      // YouTube
            "com.instagram.android",            // Instagram
            "com.zhiliaoapp.musically",         // TikTok
            "com.ss.android.ugc.trill"          // TikTok Lite
        )
    }

    // Injected dependencies
    @Inject
    lateinit var eventProcessor: AccessibilityEventProcessor
    
    @Inject
    lateinit var eventDebouncer: EventDebouncer
    
    @Inject
    lateinit var platformDetectorManager: PlatformDetectorManager
    
    @Inject
    lateinit var configurationManager: ConfigurationManager
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var errorHandler: ErrorHandler
    
    @Inject
    lateinit var safeModeManager: com.example.shortblocker.error.SafeModeManager
    
    @Inject
    lateinit var blockerLogger: com.example.shortblocker.error.BlockerLogger
    
    // BlockActionManager needs to be created manually because it requires the service instance
    private lateinit var blockActionManager: BlockActionManager

    private var isServiceRunning = false
    
    /**
     * Coroutine scope for async event processing.
     * Uses SupervisorJob to prevent child coroutine failures from canceling the entire scope.
     * Requirement 6.1, 6.2: Optimize performance with async processing
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Called when the service is connected and ready to receive events.
     * Requirement 4.1: AccessibilityService initialization
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        Log.i(TAG, "BlockerAccessibilityService connected")
        isServiceRunning = true
        
        // Initialize BlockActionManager manually (requires service instance)
        blockActionManager = BlockActionManager(
            context = applicationContext,
            accessibilityService = this,
            settingsRepository = settingsRepository
        )
        
        // Configure service info programmatically (in addition to XML config)
        val info = AccessibilityServiceInfo().apply {
            // Event types we want to monitor
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            
            // Feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            
            // Flags
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            
            // Notification timeout (milliseconds)
            notificationTimeout = 100
        }
        
        serviceInfo = info
        
        Log.i(TAG, "Service configuration applied with DI components")
    }

    /**
     * Called when an accessibility event occurs.
     * This is the main entry point for detecting short video content.
     * 
     * Requirement 4.3: Monitor screen elements and detect short video sections
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isServiceRunning || event == null) {
            return
        }

        // Filter events from target packages only
        val packageName = event.packageName?.toString() ?: return
        if (!isTargetPackage(packageName)) {
            return
        }

        // Log event for debugging
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Event received: type=${event.eventType}, " +
                      "package=$packageName, " +
                      "class=${event.className}")
        }

        // Debounce events to optimize performance and process event
        eventDebouncer.debounce(packageName) {
            processEventInternal(event)
        }
    }
    
    /**
     * Internal method to process the event after debouncing.
     * Uses coroutines for async processing to optimize performance.
     * 
     * Requirement 6.1, 6.2: Async processing with Dispatchers.Default for detection
     * and Dispatchers.Main for UI actions
     * Requirement 7.1, 7.2, 7.3, 7.4: Error handling and safe mode integration
     */
    private fun processEventInternal(event: AccessibilityEvent) {
        // Launch coroutine for async processing
        serviceScope.launch {
            try {
                // Check if service is enabled
                val isEnabled = configurationManager.isEnabled()
                if (!isEnabled) {
                    blockerLogger.logDebug(TAG, "Service is disabled, skipping event")
                    return@launch
                }
                
                // Check if temporarily disabled
                val isTemporarilyDisabled = configurationManager.isTemporarilyDisabled()
                if (isTemporarilyDisabled) {
                    blockerLogger.logDebug(TAG, "Service is temporarily disabled, skipping event")
                    return@launch
                }
                
                // Check safe mode (Requirement 7.4)
                if (safeModeManager.isInSafeModeNow()) {
                    blockerLogger.logDebug(TAG, "Safe mode active, skipping event")
                    return@launch
                }
                
                // Process event through AccessibilityEventProcessor (on Default dispatcher)
                val appContext = eventProcessor.processEvent(event)
                
                if (appContext != null) {
                    blockerLogger.logDebug(TAG, "AppContext extracted: ${appContext.packageName}, " +
                              "nodes=${appContext.nodeTree.size}")
                    
                    // Detect short video content through PlatformDetectorManager
                    // This runs on Dispatchers.Default for CPU-intensive work
                    val detectionResult = platformDetectorManager.detectShortVideo(appContext)
                    
                    if (detectionResult != null && detectionResult.isShortVideo) {
                        Log.i(TAG, "Short video detected: platform=${detectionResult.platform}, " +
                                  "method=${detectionResult.detectionMethod}, " +
                                  "confidence=${detectionResult.confidence}")
                        
                        // Log the block action (Requirement 8.2)
                        val settings = settingsRepository.getSettings()
                        blockerLogger.logBlockAction(
                            platform = detectionResult.platform,
                            detectionMethod = detectionResult.detectionMethod,
                            actionTaken = settings.blockActionType,
                            packageName = appContext.packageName
                        )
                        
                        // Execute block action on Main dispatcher (UI operations)
                        withContext(Dispatchers.Main) {
                            blockActionManager.executeBlockAction(detectionResult, appContext)
                        }
                        
                        // Reset error counter on successful detection and action
                        safeModeManager.resetErrors()
                    }
                }
            } catch (e: SecurityException) {
                // Permission errors (Requirement 7.1)
                Log.e(TAG, "Permission error processing event", e)
                val error = com.example.shortblocker.error.BlockerError.PermissionError(
                    permission = "ACCESSIBILITY_SERVICE or SYSTEM_ALERT_WINDOW",
                    message = "Permission denied: ${e.message}"
                )
                errorHandler.handleError(error)
                blockerLogger.logError(error, "Event processing failed due to permission")
            } catch (e: IllegalStateException) {
                // System state errors (Requirement 7.3)
                Log.e(TAG, "System state error processing event", e)
                val error = com.example.shortblocker.error.BlockerError.SystemError(
                    message = "Invalid system state: ${e.message}",
                    cause = e
                )
                errorHandler.handleError(error)
                blockerLogger.logError(error, "Event processing failed due to system state")
            } catch (e: Exception) {
                // General detection errors (Requirement 7.1, 7.2)
                Log.e(TAG, "Error processing event asynchronously", e)
                val error = com.example.shortblocker.error.BlockerError.DetectionError(
                    platform = com.example.shortblocker.data.Platform.UNKNOWN,
                    context = "Event processing failed",
                    cause = e
                )
                errorHandler.handleError(error)
                blockerLogger.logError(error, "Unexpected error during event processing")
            }
        }
    }

    /**
     * Called when the service is interrupted.
     * Requirement 4.2: Service lifecycle management
     */
    override fun onInterrupt() {
        Log.w(TAG, "BlockerAccessibilityService interrupted")
        // Service was interrupted, clean up if necessary
    }

    /**
     * Called when the service is being destroyed.
     * Requirement 4.2: Service lifecycle management
     */
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        
        // Cancel all coroutines
        serviceScope.cancel()
        
        // Clean up block action manager
        if (::blockActionManager.isInitialized) {
            blockActionManager.cleanup()
        }
        
        Log.i(TAG, "BlockerAccessibilityService destroyed")
    }

    /**
     * Check if the package is one of our target apps.
     */
    private fun isTargetPackage(packageName: String): Boolean {
        return TARGET_PACKAGES.contains(packageName)
    }

    /**
     * Check if the service is currently running.
     */
    fun isRunning(): Boolean {
        return isServiceRunning
    }
}

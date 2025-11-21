package com.example.shortblocker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * AccessibilityService implementation for detecting and blocking short video content.
 * 
 * This service monitors accessibility events from target apps (YouTube, Instagram, TikTok)
 * and detects when users navigate to short video sections (Shorts, Reels, etc.).
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
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

    private var isServiceRunning = false

    /**
     * Called when the service is connected and ready to receive events.
     * Requirement 4.1: AccessibilityService initialization
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        Log.i(TAG, "BlockerAccessibilityService connected")
        isServiceRunning = true
        
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
        
        Log.i(TAG, "Service configuration applied")
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

        // TODO: Process event through AccessibilityEventProcessor (Task 4)
        // TODO: Detect short video content through PlatformDetectorManager (Task 5)
        // TODO: Execute block action through BlockActionManager (Task 6)
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

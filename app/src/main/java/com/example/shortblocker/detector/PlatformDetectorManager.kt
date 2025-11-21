package com.example.shortblocker.detector

import android.util.Log
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionMethod
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform
import com.example.shortblocker.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Manager for coordinating multiple platform detectors.
 * Handles detector selection, dispatching, and error handling.
 */
class PlatformDetectorManager(
    private val settingsRepository: SettingsRepository
) {
    private val tag = "PlatformDetectorManager"
    
    private val detectors = mutableListOf<PlatformDetector>()
    
    init {
        // Register all platform detectors
        registerDetector(YouTubeDetector())
        registerDetector(InstagramDetector())
        registerDetector(TikTokDetector())
    }
    
    /**
     * Register a platform detector
     */
    fun registerDetector(detector: PlatformDetector) {
        detectors.add(detector)
        Log.d(tag, "Registered detector for platform: ${detector.platform}")
    }
    
    /**
     * Get list of enabled platforms from settings
     */
    fun getEnabledPlatforms(): Set<Platform> {
        return try {
            runBlocking {
                settingsRepository.getSettings().first().enabledPlatforms
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting enabled platforms", e)
            // Default to all platforms enabled
            setOf(Platform.YOUTUBE, Platform.INSTAGRAM, Platform.TIKTOK)
        }
    }
    
    /**
     * Detect short video from the given app context.
     * Selects appropriate detector and handles errors gracefully.
     */
    fun detectShortVideo(context: AppContext): DetectionResult? {
        // Get enabled platforms
        val enabledPlatforms = getEnabledPlatforms()
        
        // Find detector that can handle this package
        val detector = detectors.firstOrNull { it.canHandle(context.packageName) }
        
        if (detector == null) {
            Log.d(tag, "No detector found for package: ${context.packageName}")
            return null
        }
        
        // Check if platform is enabled
        if (detector.platform !in enabledPlatforms) {
            Log.d(tag, "Platform ${detector.platform} is disabled in settings")
            return DetectionResult(
                isShortVideo = false,
                platform = detector.platform,
                confidence = 1.0f,
                detectionMethod = DetectionMethod.HEURISTIC
            )
        }
        
        // Perform detection with error handling
        return try {
            val result = detector.detectShortVideo(context)
            Log.d(tag, "Detection result for ${detector.platform}: " +
                    "isShortVideo=${result.isShortVideo}, " +
                    "confidence=${result.confidence}, " +
                    "method=${result.detectionMethod}")
            result
        } catch (e: Exception) {
            Log.e(tag, "Error during detection for ${detector.platform}", e)
            handleDetectionError(detector.platform, e)
        }
    }
    
    /**
     * Handle detection errors with fallback logic
     */
    private fun handleDetectionError(platform: Platform, error: Throwable): DetectionResult? {
        Log.e(tag, "Detection failed for $platform: ${error.message}")
        
        // Return null to indicate detection failure
        // The caller can decide whether to proceed or not
        return null
    }
    
    /**
     * Get detector for a specific platform (for testing/debugging)
     */
    fun getDetectorForPlatform(platform: Platform): PlatformDetector? {
        return detectors.firstOrNull { it.platform == platform }
    }
    
    /**
     * Get all registered detectors
     */
    fun getAllDetectors(): List<PlatformDetector> {
        return detectors.toList()
    }
}

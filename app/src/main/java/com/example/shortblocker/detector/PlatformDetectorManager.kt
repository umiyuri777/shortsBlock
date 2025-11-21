package com.example.shortblocker.detector

import android.util.Log
import android.util.LruCache
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionMethod
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform
import com.example.shortblocker.data.SettingsRepository
import kotlinx.coroutines.runBlocking

/**
 * Manager for coordinating multiple platform detectors.
 * Handles detector selection, dispatching, and error handling.
 * Includes LruCache for performance optimization.
 */
class PlatformDetectorManager(
    private val settingsRepository: SettingsRepository,
    cacheSize: Int = 20
) {
    private val tag = "PlatformDetectorManager"
    
    private val detectors = mutableListOf<PlatformDetector>()
    
    /**
     * LRU cache for detection results.
     * Key format: "packageName:activityName"
     * This reduces redundant detection operations for the same context.
     */
    private val detectionCache = LruCache<String, DetectionResult>(cacheSize)
    
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
     * Get list of enabled platforms from settings.
     * Uses runBlocking since this is called from non-suspend context.
     * The actual settings retrieval is fast due to caching in the repository.
     */
    fun getEnabledPlatforms(): Set<Platform> {
        return try {
            runBlocking {
                settingsRepository.getSettings().enabledPlatforms
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
     * Uses cache to avoid redundant detection operations.
     * 
     * This method is designed to be called from background threads/coroutines
     * for optimal performance (Requirement 6.1, 6.2).
     */
    fun detectShortVideo(context: AppContext): DetectionResult? {
        // Generate cache key from package name and activity name
        val cacheKey = generateCacheKey(context)
        
        // Check cache first
        detectionCache.get(cacheKey)?.let { cachedResult ->
            Log.d(tag, "Cache hit for key: $cacheKey")
            return cachedResult
        }
        
        Log.d(tag, "Cache miss for key: $cacheKey, performing detection")
        
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
            val result = DetectionResult(
                isShortVideo = false,
                platform = detector.platform,
                confidence = 1.0f,
                detectionMethod = DetectionMethod.HEURISTIC
            )
            // Cache the result
            detectionCache.put(cacheKey, result)
            return result
        }
        
        // Perform detection with error handling
        return try {
            val result = detector.detectShortVideo(context)
            Log.d(tag, "Detection result for ${detector.platform}: " +
                    "isShortVideo=${result.isShortVideo}, " +
                    "confidence=${result.confidence}, " +
                    "method=${result.detectionMethod}")
            
            // Cache the result
            detectionCache.put(cacheKey, result)
            result
        } catch (e: Exception) {
            Log.e(tag, "Error during detection for ${detector.platform}", e)
            handleDetectionError(detector.platform, e)
        }
    }
    
    /**
     * Generate cache key from app context.
     * Format: "packageName:activityName"
     */
    private fun generateCacheKey(context: AppContext): String {
        return "${context.packageName}:${context.activityName ?: "unknown"}"
    }
    
    /**
     * Clear the detection cache.
     * Useful when settings change or for testing.
     */
    fun clearCache() {
        detectionCache.evictAll()
        Log.d(tag, "Detection cache cleared")
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    fun getCacheStats(): Pair<Int, Int> {
        return Pair(detectionCache.size(), detectionCache.maxSize())
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

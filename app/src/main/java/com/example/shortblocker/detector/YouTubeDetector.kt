package com.example.shortblocker.detector

import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionMethod
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform

/**
 * Detector for YouTube Shorts.
 * Uses multiple detection methods with priority-based logic:
 * 1. UI element detection (highest priority)
 * 2. URL pattern detection
 * 3. Activity name detection
 */
class YouTubeDetector : PlatformDetector {
    
    override val platform = Platform.YOUTUBE
    
    private val youtubePackages = setOf(
        "com.google.android.youtube",
        "com.google.android.youtube.tv"
    )
    
    // UI element indicators for Shorts
    private val shortsResourceIds = listOf(
        "shorts_player_fragment",
        "reel_player_page_container",
        "shorts_container",
        "reel_watch_player"
    )
    
    // URL pattern for Shorts
    private val shortsUrlPattern = "/shorts/"
    
    // Activity name patterns
    private val shortsActivityPatterns = listOf(
        "short",
        "reel"
    )
    
    override fun canHandle(packageName: String): Boolean {
        return packageName in youtubePackages
    }
    
    override fun detectShortVideo(context: AppContext): DetectionResult {
        // Priority 1: UI element detection
        val uiDetection = detectFromUIElements(context)
        if (uiDetection.isShortVideo) {
            return uiDetection
        }
        
        // Priority 2: URL pattern detection
        val urlDetection = detectFromURLPattern(context)
        if (urlDetection.isShortVideo) {
            return urlDetection
        }
        
        // Priority 3: Activity name detection
        val activityDetection = detectFromActivityName(context)
        if (activityDetection.isShortVideo) {
            return activityDetection
        }
        
        // No shorts detected
        return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.HEURISTIC
        )
    }
    
    /**
     * Detect Shorts from UI elements (resource IDs)
     * Highest confidence method
     */
    private fun detectFromUIElements(context: AppContext): DetectionResult {
        val nodes = context.nodeTree
        
        for (resourceId in shortsResourceIds) {
            val matchingNodes = findNodesByResourceId(nodes, resourceId)
            if (matchingNodes.isNotEmpty()) {
                return DetectionResult(
                    isShortVideo = true,
                    platform = platform,
                    confidence = 0.95f,
                    detectionMethod = DetectionMethod.UI_ELEMENT
                )
            }
        }
        
        // Check for "Shorts" tab text
        val shortsTabNodes = findNodesByText(nodes, "Shorts")
        if (shortsTabNodes.isNotEmpty()) {
            // Verify it's a selected/active tab
            val isSelected = shortsTabNodes.any { node ->
                node.isSelected || node.isFocused
            }
            if (isSelected) {
                return DetectionResult(
                    isShortVideo = true,
                    platform = platform,
                    confidence = 0.85f,
                    detectionMethod = DetectionMethod.UI_ELEMENT
                )
            }
        }
        
        return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.UI_ELEMENT
        )
    }
    
    /**
     * Detect Shorts from URL patterns in text nodes
     * Medium confidence method
     */
    private fun detectFromURLPattern(context: AppContext): DetectionResult {
        val nodes = context.nodeTree
        
        val urlNodes = findNodesByText(nodes, shortsUrlPattern)
        if (urlNodes.isNotEmpty()) {
            return DetectionResult(
                isShortVideo = true,
                platform = platform,
                confidence = 0.80f,
                detectionMethod = DetectionMethod.URL_PATTERN
            )
        }
        
        return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.URL_PATTERN
        )
    }
    
    /**
     * Detect Shorts from activity name
     * Lower confidence method (fallback)
     */
    private fun detectFromActivityName(context: AppContext): DetectionResult {
        val activityName = context.activityName ?: return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.ACTIVITY_NAME
        )
        
        for (pattern in shortsActivityPatterns) {
            if (activityMatches(activityName, pattern)) {
                return DetectionResult(
                    isShortVideo = true,
                    platform = platform,
                    confidence = 0.70f,
                    detectionMethod = DetectionMethod.ACTIVITY_NAME
                )
            }
        }
        
        return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.ACTIVITY_NAME
        )
    }
}

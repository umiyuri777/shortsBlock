package com.example.shortblocker.detector

import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionMethod
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform

/**
 * Detector for Instagram Reels.
 * Uses multiple detection methods:
 * 1. UI element detection (resource IDs)
 * 2. Reels tab detection
 * 3. Activity name detection
 */
class InstagramDetector : PlatformDetector {
    
    override val platform = Platform.INSTAGRAM
    
    private val instagramPackages = setOf(
        "com.instagram.android",
        "com.instagram.lite"
    )
    
    // UI element indicators for Reels
    private val reelsResourceIds = listOf(
        "clips_viewer_view_pager",
        "clips_viewer",
        "reels_viewer",
        "clips_viewer_container",
        "clips_tab"
    )
    
    // Reels tab indicators
    private val reelsTabIndicators = listOf(
        "Reels",
        "reels"
    )
    
    // Activity name patterns
    private val reelsActivityPatterns = listOf(
        "reels",
        "clips"
    )
    
    override fun canHandle(packageName: String): Boolean {
        return packageName in instagramPackages
    }
    
    override fun detectShortVideo(context: AppContext): DetectionResult {
        // Priority 1: UI element detection
        val uiDetection = detectFromUIElements(context)
        if (uiDetection.isShortVideo) {
            return uiDetection
        }
        
        // Priority 2: Reels tab detection
        val tabDetection = detectFromReelsTab(context)
        if (tabDetection.isShortVideo) {
            return tabDetection
        }
        
        // Priority 3: Activity name detection
        val activityDetection = detectFromActivityName(context)
        if (activityDetection.isShortVideo) {
            return activityDetection
        }
        
        // No reels detected
        return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.HEURISTIC
        )
    }
    
    /**
     * Detect Reels from UI elements (resource IDs)
     * Highest confidence method
     */
    private fun detectFromUIElements(context: AppContext): DetectionResult {
        val nodes = context.nodeTree
        
        for (resourceId in reelsResourceIds) {
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
        
        return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.UI_ELEMENT
        )
    }
    
    /**
     * Detect Reels from tab selection
     * Medium-high confidence method
     */
    private fun detectFromReelsTab(context: AppContext): DetectionResult {
        val nodes = context.nodeTree
        
        // Look for Reels tab by text
        for (indicator in reelsTabIndicators) {
            val tabNodes = findNodesByText(nodes, indicator)
            if (tabNodes.isNotEmpty()) {
                // Check if tab is selected
                val isSelected = tabNodes.any { node ->
                    node.isSelected || node.isFocused
                }
                if (isSelected) {
                    return DetectionResult(
                        isShortVideo = true,
                        platform = platform,
                        confidence = 0.90f,
                        detectionMethod = DetectionMethod.UI_ELEMENT
                    )
                }
            }
            
            // Look for Reels tab by content description
            val descriptionNodes = findNodesByContentDescription(nodes, indicator)
            if (descriptionNodes.isNotEmpty()) {
                val isSelected = descriptionNodes.any { node ->
                    node.isSelected || node.isFocused
                }
                if (isSelected) {
                    return DetectionResult(
                        isShortVideo = true,
                        platform = platform,
                        confidence = 0.90f,
                        detectionMethod = DetectionMethod.UI_ELEMENT
                    )
                }
            }
        }
        
        // Check for ImageView with Reels content description (tab icon)
        val imageViews = findNodesByClassName(nodes, "android.widget.ImageView")
        for (imageView in imageViews) {
            val contentDesc = imageView.contentDescription?.toString() ?: continue
            if (reelsTabIndicators.any { contentDesc.contains(it, ignoreCase = true) }) {
                if (imageView.isSelected || imageView.isFocused) {
                    return DetectionResult(
                        isShortVideo = true,
                        platform = platform,
                        confidence = 0.85f,
                        detectionMethod = DetectionMethod.UI_ELEMENT
                    )
                }
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
     * Detect Reels from activity name
     * Lower confidence method (fallback)
     */
    private fun detectFromActivityName(context: AppContext): DetectionResult {
        val activityName = context.activityName ?: return DetectionResult(
            isShortVideo = false,
            platform = platform,
            confidence = 1.0f,
            detectionMethod = DetectionMethod.ACTIVITY_NAME
        )
        
        for (pattern in reelsActivityPatterns) {
            if (activityMatches(activityName, pattern)) {
                return DetectionResult(
                    isShortVideo = true,
                    platform = platform,
                    confidence = 0.75f,
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

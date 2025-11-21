package com.example.shortblocker.detector

import android.view.accessibility.AccessibilityNodeInfo
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform

/**
 * Interface for platform-specific short video detection logic.
 * Each platform (YouTube, Instagram, TikTok) implements this interface
 * to provide custom detection strategies.
 */
interface PlatformDetector {
    /**
     * The platform this detector handles
     */
    val platform: Platform
    
    /**
     * Check if this detector can handle the given package name
     * @param packageName The package name of the app
     * @return true if this detector can handle the package
     */
    fun canHandle(packageName: String): Boolean
    
    /**
     * Detect if the current context represents a short video section
     * @param context The current app context with node tree
     * @return DetectionResult indicating if short video was detected
     */
    fun detectShortVideo(context: AppContext): DetectionResult
    
    /**
     * Utility: Find nodes by resource ID
     */
    fun findNodesByResourceId(
        nodes: List<AccessibilityNodeInfo>,
        resourceId: String
    ): List<AccessibilityNodeInfo> {
        return nodes.filter { node ->
            node.viewIdResourceName?.contains(resourceId, ignoreCase = true) == true
        }
    }
    
    /**
     * Utility: Find nodes by text content
     */
    fun findNodesByText(
        nodes: List<AccessibilityNodeInfo>,
        text: String,
        ignoreCase: Boolean = true
    ): List<AccessibilityNodeInfo> {
        return nodes.filter { node ->
            node.text?.toString()?.contains(text, ignoreCase) == true
        }
    }
    
    /**
     * Utility: Find nodes by content description
     */
    fun findNodesByContentDescription(
        nodes: List<AccessibilityNodeInfo>,
        description: String,
        ignoreCase: Boolean = true
    ): List<AccessibilityNodeInfo> {
        return nodes.filter { node ->
            node.contentDescription?.toString()?.contains(description, ignoreCase) == true
        }
    }
    
    /**
     * Utility: Find nodes by class name
     */
    fun findNodesByClassName(
        nodes: List<AccessibilityNodeInfo>,
        className: String
    ): List<AccessibilityNodeInfo> {
        return nodes.filter { node ->
            node.className?.toString() == className
        }
    }
    
    /**
     * Utility: Check if activity name matches pattern
     */
    fun activityMatches(activityName: String?, pattern: String, ignoreCase: Boolean = true): Boolean {
        return activityName?.contains(pattern, ignoreCase) == true
    }
}

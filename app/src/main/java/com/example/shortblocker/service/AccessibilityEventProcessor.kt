package com.example.shortblocker.service

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.shortblocker.data.AppContext

/**
 * Processes AccessibilityEvents and extracts relevant context information.
 * 
 * This class is responsible for:
 * - Extracting AppContext from AccessibilityEvents
 * - Building AccessibilityNodeInfo trees
 * - Filtering events to only process target packages
 * 
 * Requirements: 4.3, 6.1, 6.3
 */
interface AccessibilityEventProcessor {
    /**
     * Process an accessibility event and extract app context.
     * Returns null if the event should be ignored.
     * 
     * @param event The accessibility event to process
     * @return AppContext if event is relevant, null otherwise
     */
    fun processEvent(event: AccessibilityEvent): AppContext?
    
    /**
     * Extract node information from an accessibility event.
     * 
     * @param event The accessibility event
     * @return List of AccessibilityNodeInfo from the event
     */
    fun extractNodeInfo(event: AccessibilityEvent): List<AccessibilityNodeInfo>
}

/**
 * Default implementation of AccessibilityEventProcessor.
 */
class DefaultAccessibilityEventProcessor(
    private val targetPackages: Set<String>
) : AccessibilityEventProcessor {
    
    companion object {
        private const val TAG = "EventProcessor"
        
        // Relevant event types for detection
        private val RELEVANT_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )
    }
    
    /**
     * Process event and extract context.
     * Implements filtering logic to only process target packages.
     * 
     * Requirement 4.3: Event filtering for target packages only
     * Requirement 6.1: Minimize processing for non-target apps
     */
    override fun processEvent(event: AccessibilityEvent): AppContext? {
        // Filter by event type
        if (event.eventType !in RELEVANT_EVENT_TYPES) {
            return null
        }
        
        // Extract package name
        val packageName = event.packageName?.toString()
        if (packageName.isNullOrEmpty()) {
            return null
        }
        
        // Filter by target packages (Requirement 6.3)
        if (!isTargetPackage(packageName)) {
            return null
        }
        
        // Extract activity name from className
        val activityName = event.className?.toString()
        
        // Extract node tree
        val nodeTree = extractNodeInfo(event)
        
        // Create and return AppContext
        val context = AppContext(
            packageName = packageName,
            activityName = activityName,
            nodeTree = nodeTree,
            timestamp = System.currentTimeMillis()
        )
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Processed event: package=$packageName, " +
                      "activity=$activityName, nodes=${nodeTree.size}")
        }
        
        return context
    }
    
    /**
     * Extract AccessibilityNodeInfo tree from event.
     * Builds a complete tree of nodes for analysis.
     * 
     * Requirement 4.3: AccessibilityNodeInfo tree construction
     */
    override fun extractNodeInfo(event: AccessibilityEvent): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        
        try {
            // Get root node from event source
            val source = event.source
            if (source != null) {
                // Traverse the node tree recursively
                collectNodes(source, nodes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting node info", e)
        }
        
        return nodes
    }
    
    /**
     * Recursively collect all nodes in the tree.
     * 
     * @param node The current node to process
     * @param collector List to collect nodes into
     */
    private fun collectNodes(
        node: AccessibilityNodeInfo,
        collector: MutableList<AccessibilityNodeInfo>
    ) {
        try {
            // Add current node
            collector.add(node)
            
            // Recursively process children
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    collectNodes(child, collector)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting nodes", e)
        }
    }
    
    /**
     * Check if package is in target list.
     */
    private fun isTargetPackage(packageName: String): Boolean {
        return targetPackages.contains(packageName)
    }
}

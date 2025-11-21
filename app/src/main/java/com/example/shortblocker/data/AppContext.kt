package com.example.shortblocker.data

import android.view.accessibility.AccessibilityNodeInfo

data class AppContext(
    val packageName: String,
    val activityName: String?,
    val nodeTree: List<AccessibilityNodeInfo>,
    val timestamp: Long = System.currentTimeMillis()
)

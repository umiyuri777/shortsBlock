package com.example.shortblocker.data

data class DetectionResult(
    val isShortVideo: Boolean,
    val platform: Platform,
    val confidence: Float,
    val detectionMethod: DetectionMethod
)

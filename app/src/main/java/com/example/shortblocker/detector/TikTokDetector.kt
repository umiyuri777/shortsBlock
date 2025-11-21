package com.example.shortblocker.detector

import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionMethod
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform

/**
 * Detector for TikTok.
 * Since TikTok is entirely a short video platform, we simply detect
 * when the app is launched/active.
 */
class TikTokDetector : PlatformDetector {
    
    override val platform = Platform.TIKTOK
    
    private val tiktokPackages = setOf(
        "com.zhiliaoapp.musically",  // TikTok
        "com.ss.android.ugc.trill"   // TikTok Lite
    )
    
    override fun canHandle(packageName: String): Boolean {
        return packageName in tiktokPackages
    }
    
    override fun detectShortVideo(context: AppContext): DetectionResult {
        // TikTok is entirely short-form video content
        // If the app is active, we consider it as short video
        return if (canHandle(context.packageName)) {
            DetectionResult(
                isShortVideo = true,
                platform = platform,
                confidence = 1.0f,
                detectionMethod = DetectionMethod.HEURISTIC
            )
        } else {
            DetectionResult(
                isShortVideo = false,
                platform = platform,
                confidence = 1.0f,
                detectionMethod = DetectionMethod.HEURISTIC
            )
        }
    }
}

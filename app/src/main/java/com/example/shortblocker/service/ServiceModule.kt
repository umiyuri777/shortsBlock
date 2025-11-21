package com.example.shortblocker.service

import com.example.shortblocker.action.BlockActionManager
import com.example.shortblocker.config.ConfigurationManager
import com.example.shortblocker.detector.PlatformDetectorManager
import com.example.shortblocker.error.ErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for accessibility service components.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideAccessibilityEventProcessor(): AccessibilityEventProcessor {
        val targetPackages = setOf(
            "com.google.android.youtube",      // YouTube
            "com.instagram.android",            // Instagram
            "com.zhiliaoapp.musically",         // TikTok
            "com.ss.android.ugc.trill"          // TikTok Lite
        )
        return DefaultAccessibilityEventProcessor(targetPackages)
    }

    @Provides
    @Singleton
    fun provideEventDebouncer(): EventDebouncer {
        return EventDebouncer(delayMs = 100)
    }
}

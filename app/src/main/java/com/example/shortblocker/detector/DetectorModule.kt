package com.example.shortblocker.detector

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for platform detector components.
 */
@Module
@InstallIn(SingletonComponent::class)
object DetectorModule {

    @Provides
    @Singleton
    fun providePlatformDetectorManager(
        settingsRepository: com.example.shortblocker.data.SettingsRepository
    ): PlatformDetectorManager {
        return PlatformDetectorManager(settingsRepository)
    }
}

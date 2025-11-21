package com.example.shortblocker.ui

import com.example.shortblocker.data.BlockLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for UI components.
 */
@Module
@InstallIn(SingletonComponent::class)
object UIModule {

    @Provides
    @Singleton
    fun provideStatisticsManager(
        blockLogDao: BlockLogDao
    ): StatisticsManager {
        return StatisticsManager(blockLogDao)
    }
}

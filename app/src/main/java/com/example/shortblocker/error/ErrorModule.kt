package com.example.shortblocker.error

import android.content.Context
import com.example.shortblocker.data.BlockLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for error handling components.
 */
@Module
@InstallIn(SingletonComponent::class)
object ErrorModule {

    @Provides
    @Singleton
    fun provideSafeModeManager(
        @ApplicationContext context: Context
    ): SafeModeManager {
        return SafeModeManager(context)
    }

    @Provides
    @Singleton
    fun provideErrorHandler(
        @ApplicationContext context: Context,
        safeModeManager: SafeModeManager
    ): ErrorHandler {
        return DefaultErrorHandler(context, safeModeManager)
    }

    @Provides
    @Singleton
    fun provideBlockerLogger(
        blockLogDao: BlockLogDao
    ): BlockerLogger {
        return BlockerLogger(blockLogDao)
    }
}

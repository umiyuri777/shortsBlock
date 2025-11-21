package com.example.shortblocker.config

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.example.shortblocker.data.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {
    
    @Provides
    @Singleton
    fun provideConfigurationManager(
        settingsRepository: SettingsRepository
    ): ConfigurationManager {
        return ConfigurationManagerImpl(settingsRepository)
    }
    
    @Provides
    @Singleton
    fun provideTemporaryDisableManager(
        @ApplicationContext context: Context,
        configurationManager: ConfigurationManager
    ): TemporaryDisableManager {
        return TemporaryDisableManager(context, configurationManager)
    }
}

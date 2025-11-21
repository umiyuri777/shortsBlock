package com.example.shortblocker.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveSettings(settings: AppSettings)
    suspend fun getSettings(): AppSettings
    fun observeSettings(): Flow<AppSettings>
    suspend fun updatePlatformEnabled(platform: Platform, enabled: Boolean)
    suspend fun updateBlockActionType(actionType: BlockActionType)
    suspend fun updateServiceEnabled(enabled: Boolean)
    suspend fun setTemporaryDisable(endTime: Long?)
}

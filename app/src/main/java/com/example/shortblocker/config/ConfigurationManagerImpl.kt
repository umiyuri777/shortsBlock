package com.example.shortblocker.config

import com.example.shortblocker.data.AppSettings
import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.Platform
import com.example.shortblocker.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationManagerImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ConfigurationManager {
    
    override suspend fun isEnabled(): Boolean {
        val settings = settingsRepository.getSettings()
        return settings.isEnabled && !isTemporarilyDisabled()
    }
    
    override suspend fun isPlatformEnabled(platform: Platform): Boolean {
        val settings = settingsRepository.getSettings()
        return settings.enabledPlatforms.contains(platform)
    }
    
    override suspend fun getBlockActionType(): BlockActionType {
        return settingsRepository.getSettings().blockActionType
    }
    
    override suspend fun getTemporaryDisableEndTime(): Long? {
        return settingsRepository.getSettings().temporaryDisableEndTime
    }
    
    override suspend fun isTemporarilyDisabled(): Boolean {
        val endTime = getTemporaryDisableEndTime() ?: return false
        val currentTime = System.currentTimeMillis()
        
        // If the end time has passed, clear the temporary disable
        if (currentTime >= endTime) {
            clearTemporaryDisable()
            return false
        }
        
        return true
    }
    
    override suspend fun setTemporaryDisable(durationMinutes: Int) {
        if (durationMinutes <= 0) {
            clearTemporaryDisable()
            return
        }
        
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        settingsRepository.setTemporaryDisable(endTime)
    }
    
    override suspend fun setServiceEnabled(enabled: Boolean) {
        settingsRepository.updateServiceEnabled(enabled)
    }
    
    override suspend fun setPlatformEnabled(platform: Platform, enabled: Boolean) {
        settingsRepository.updatePlatformEnabled(platform, enabled)
    }
    
    override suspend fun setBlockActionType(actionType: BlockActionType) {
        settingsRepository.updateBlockActionType(actionType)
    }
    
    override fun observeEnabled(): Flow<Boolean> {
        return settingsRepository.observeSettings().map { settings ->
            val temporarilyDisabled = settings.temporaryDisableEndTime?.let { endTime ->
                System.currentTimeMillis() < endTime
            } ?: false
            
            settings.isEnabled && !temporarilyDisabled
        }
    }
    
    override suspend fun clearTemporaryDisable() {
        settingsRepository.setTemporaryDisable(null)
    }
    
    override suspend fun setEnabled(enabled: Boolean) {
        setServiceEnabled(enabled)
    }
    
    override fun getSettings(): Flow<AppSettings> {
        return settingsRepository.observeSettings()
    }
}

package com.example.shortblocker.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val sharedPreferences: SharedPreferences
) : SettingsRepository {

    companion object {
        private const val PREF_KEY_FIRST_LAUNCH = "first_launch"
        private const val PREF_KEY_SHOW_TUTORIAL = "show_tutorial"
    }

    override suspend fun saveSettings(settings: AppSettings) {
        val entity = settings.toEntity()
        settingsDao.insertSettings(entity)
    }

    override suspend fun getSettings(): AppSettings {
        val entity = settingsDao.getSettings() ?: createDefaultSettings()
        return entity.toAppSettings()
    }

    override fun observeSettings(): Flow<AppSettings> {
        return settingsDao.observeSettings().map { entity ->
            entity?.toAppSettings() ?: AppSettings()
        }
    }

    override suspend fun updatePlatformEnabled(platform: Platform, enabled: Boolean) {
        val currentSettings = getSettings()
        val updatedPlatforms = if (enabled) {
            currentSettings.enabledPlatforms + platform
        } else {
            currentSettings.enabledPlatforms - platform
        }
        saveSettings(currentSettings.copy(enabledPlatforms = updatedPlatforms))
    }

    override suspend fun updateBlockActionType(actionType: BlockActionType) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(blockActionType = actionType))
    }

    override suspend fun updateServiceEnabled(enabled: Boolean) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(isEnabled = enabled))
    }

    override suspend fun setTemporaryDisable(endTime: Long?) {
        val currentSettings = getSettings()
        saveSettings(currentSettings.copy(temporaryDisableEndTime = endTime))
    }

    private suspend fun createDefaultSettings(): SettingsEntity {
        val defaultEntity = SettingsEntity()
        settingsDao.insertSettings(defaultEntity)
        return defaultEntity
    }

    // Extension functions for conversion
    private fun AppSettings.toEntity(): SettingsEntity {
        val platformsJson = JSONArray().apply {
            enabledPlatforms.forEach { put(it.name) }
        }.toString()

        return SettingsEntity(
            id = 1,
            isEnabled = isEnabled,
            enabledPlatforms = platformsJson,
            blockActionType = blockActionType.name,
            temporaryDisableEndTime = temporaryDisableEndTime
        )
    }

    private fun SettingsEntity.toAppSettings(): AppSettings {
        val platforms = try {
            val jsonArray = JSONArray(enabledPlatforms)
            (0 until jsonArray.length()).mapNotNull { index ->
                try {
                    Platform.valueOf(jsonArray.getString(index))
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
        } catch (e: Exception) {
            setOf(Platform.YOUTUBE, Platform.INSTAGRAM, Platform.TIKTOK)
        }

        val actionType = try {
            BlockActionType.valueOf(blockActionType)
        } catch (e: IllegalArgumentException) {
            BlockActionType.NAVIGATE_BACK
        }

        return AppSettings(
            isEnabled = isEnabled,
            enabledPlatforms = platforms,
            blockActionType = actionType,
            temporaryDisableEndTime = temporaryDisableEndTime
        )
    }
}

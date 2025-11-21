package com.example.shortblocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isEnabled: Boolean = true,
    val enabledPlatforms: String = "[]", // JSON array of platform names
    val blockActionType: String = "NAVIGATE_BACK",
    val temporaryDisableEndTime: Long? = null
)

package com.example.shortblocker.data

data class AppSettings(
    val isEnabled: Boolean = true,
    val enabledPlatforms: Set<Platform> = setOf(Platform.YOUTUBE, Platform.INSTAGRAM, Platform.TIKTOK),
    val blockActionType: BlockActionType = BlockActionType.NAVIGATE_BACK,
    val temporaryDisableEndTime: Long? = null
)

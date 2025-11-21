package com.example.shortblocker.action

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dagger Hilt module for block action components.
 * 
 * Note: BlockActionManager requires an AccessibilityService instance which cannot be
 * injected via Hilt. It must be created manually in the AccessibilityService.
 */
@Module
@InstallIn(SingletonComponent::class)
object ActionModule {
    // BlockActionManager is created manually in BlockerAccessibilityService
    // because it requires the AccessibilityService instance
}

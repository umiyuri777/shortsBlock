package com.example.shortblocker.config

import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.Platform
import kotlinx.coroutines.flow.Flow

interface ConfigurationManager {
    /**
     * Check if the blocker service is enabled
     */
    suspend fun isEnabled(): Boolean
    
    /**
     * Check if a specific platform is enabled for blocking
     */
    suspend fun isPlatformEnabled(platform: Platform): Boolean
    
    /**
     * Get the current block action type
     */
    suspend fun getBlockActionType(): BlockActionType
    
    /**
     * Get the temporary disable end time (null if not temporarily disabled)
     */
    suspend fun getTemporaryDisableEndTime(): Long?
    
    /**
     * Check if the service is currently temporarily disabled
     */
    suspend fun isTemporarilyDisabled(): Boolean
    
    /**
     * Set temporary disable for a specified duration in minutes
     * @param durationMinutes Duration in minutes (0 to cancel temporary disable)
     */
    suspend fun setTemporaryDisable(durationMinutes: Int)
    
    /**
     * Enable or disable the blocker service
     */
    suspend fun setServiceEnabled(enabled: Boolean)
    
    /**
     * Enable or disable a specific platform
     */
    suspend fun setPlatformEnabled(platform: Platform, enabled: Boolean)
    
    /**
     * Set the block action type
     */
    suspend fun setBlockActionType(actionType: BlockActionType)
    
    /**
     * Observe configuration changes
     */
    fun observeEnabled(): Flow<Boolean>
    
    /**
     * Clear temporary disable (re-enable the service)
     */
    suspend fun clearTemporaryDisable()
}

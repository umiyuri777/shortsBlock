package com.example.shortblocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shortblocker.config.ConfigurationManager
import com.example.shortblocker.data.AppSettings
import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.Platform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the main screen
 */
data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val todayBlocks: Int = 0,
    val weekBlocks: Int = 0,
    val isTemporarilyDisabled: Boolean = false,
    val temporaryDisableEndTime: Long? = null,
    val isLoading: Boolean = true
)

/**
 * ViewModel for MainActivity
 * Manages settings, statistics, and temporary disable functionality
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val configurationManager: ConfigurationManager,
    private val statisticsManager: StatisticsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    /**
     * Combined flow of settings and statistics
     */
    val mainUiState: StateFlow<MainUiState> = combine(
        configurationManager.getSettings(),
        statisticsManager.observeTodayBlockCount(),
        statisticsManager.observeWeekBlockCount()
    ) { settings, todayBlocks, weekBlocks ->
        val isTemporarilyDisabled = settings.temporaryDisableEndTime?.let { 
            it > System.currentTimeMillis() 
        } ?: false
        
        MainUiState(
            settings = settings,
            todayBlocks = todayBlocks,
            weekBlocks = weekBlocks,
            isTemporarilyDisabled = isTemporarilyDisabled,
            temporaryDisableEndTime = settings.temporaryDisableEndTime,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )
    
    /**
     * Enable or disable the blocker service
     */
    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configurationManager.setEnabled(enabled)
        }
    }
    
    /**
     * Enable or disable a specific platform
     */
    fun setPlatformEnabled(platform: Platform, enabled: Boolean) {
        viewModelScope.launch {
            configurationManager.setPlatformEnabled(platform, enabled)
        }
    }
    
    /**
     * Set the block action type
     */
    fun setBlockActionType(actionType: BlockActionType) {
        viewModelScope.launch {
            configurationManager.setBlockActionType(actionType)
        }
    }
    
    /**
     * Temporarily disable the blocker for a specified duration
     * @param durationMinutes Duration in minutes
     */
    fun setTemporaryDisable(durationMinutes: Int) {
        viewModelScope.launch {
            configurationManager.setTemporaryDisable(durationMinutes)
        }
    }
    
    /**
     * Clear temporary disable and re-enable the blocker
     */
    fun clearTemporaryDisable() {
        viewModelScope.launch {
            configurationManager.clearTemporaryDisable()
        }
    }
    
    /**
     * Get today's statistics
     */
    suspend fun getTodayStatistics(): BlockStatistics {
        return statisticsManager.getTodayStatistics()
    }
    
    /**
     * Get week's statistics
     */
    suspend fun getWeekStatistics(): BlockStatistics {
        return statisticsManager.getWeekStatistics()
    }
    
    /**
     * Get month's statistics
     */
    suspend fun getMonthStatistics(): BlockStatistics {
        return statisticsManager.getMonthStatistics()
    }
    
    /**
     * Get daily statistics for the last N days
     */
    suspend fun getDailyStatistics(days: Int): List<Pair<Long, Int>> {
        return statisticsManager.getDailyStatistics(days)
    }
    
    /**
     * Refresh statistics manually
     */
    fun refreshStatistics() {
        viewModelScope.launch {
            val todayStats = statisticsManager.getTodayStatistics()
            val weekStats = statisticsManager.getWeekStatistics()
            
            _uiState.value = _uiState.value.copy(
                todayBlocks = todayStats.totalBlocks,
                weekBlocks = weekStats.totalBlocks
            )
        }
    }
}

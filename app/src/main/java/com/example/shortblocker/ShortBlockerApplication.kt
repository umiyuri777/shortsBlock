package com.example.shortblocker

import android.app.Application
import android.util.Log
import com.example.shortblocker.config.ConfigurationManager
import com.example.shortblocker.data.AppDatabase
import com.example.shortblocker.data.AppSettings
import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.Platform
import com.example.shortblocker.data.SettingsRepository
import com.example.shortblocker.error.BlockerLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Short Video Blocker.
 * Handles app-wide initialization including database setup and default settings.
 * 
 * Requirements: 4.1, 4.5, 5.1
 */
@HiltAndroidApp
class ShortBlockerApplication : Application() {
    
    companion object {
        private const val TAG = "ShortBlockerApp"
        private const val PREFS_NAME = "short_blocker_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
    
    @Inject
    lateinit var database: AppDatabase
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var configurationManager: ConfigurationManager
    
    @Inject
    lateinit var blockerLogger: BlockerLogger
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Called when the application is starting.
     * Initializes database, creates default settings, and performs first-launch setup.
     * 
     * Requirement 4.1: Application initialization
     * Requirement 5.1: Default settings creation
     */
    override fun onCreate() {
        super.onCreate()
        
        Log.i(TAG, "Short Video Blocker Application starting...")
        
        // Initialize database and settings
        applicationScope.launch {
            try {
                initializeDatabase()
                initializeDefaultSettings()
                performFirstLaunchSetup()
                
                Log.i(TAG, "Application initialization completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during application initialization", e)
            }
        }
    }
    
    /**
     * Initialize the database.
     * Ensures database is created and ready for use.
     * 
     * Requirement 4.1: Database initialization
     */
    private suspend fun initializeDatabase() {
        try {
            // Trigger database creation by accessing DAOs
            database.settingsDao()
            database.blockLogDao()
            
            Log.i(TAG, "Database initialized successfully")
            blockerLogger.logInfo(TAG, "Database initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize database", e)
            throw e
        }
    }
    
    /**
     * Initialize default settings if they don't exist.
     * Creates default configuration for first-time users.
     * 
     * Requirement 5.1: Default settings creation
     */
    private suspend fun initializeDefaultSettings() {
        try {
            // Check if settings already exist
            val existingSettings = settingsRepository.getSettings()
            
            if (existingSettings.enabledPlatforms.isEmpty()) {
                // Create default settings
                val defaultSettings = AppSettings(
                    isEnabled = false, // Disabled by default until user grants permissions
                    enabledPlatforms = setOf(
                        Platform.YOUTUBE,
                        Platform.INSTAGRAM,
                        Platform.TIKTOK
                    ),
                    blockActionType = BlockActionType.NAVIGATE_BACK, // Safe default
                    temporaryDisableEndTime = null
                )
                
                settingsRepository.saveSettings(defaultSettings)
                
                Log.i(TAG, "Default settings created: $defaultSettings")
                blockerLogger.logInfo(TAG, "Default settings initialized")
            } else {
                Log.i(TAG, "Existing settings found, skipping default creation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default settings", e)
            throw e
        }
    }
    
    /**
     * Perform first-launch setup tasks.
     * Checks if this is the first time the app is launched and performs necessary setup.
     * 
     * Requirement 4.5: First launch detection
     */
    private fun performFirstLaunchSetup() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
            
            if (isFirstLaunch) {
                Log.i(TAG, "First launch detected, performing initial setup")
                
                // Mark first launch as complete
                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                
                // Log first launch
                blockerLogger.logInfo(TAG, "First launch setup completed")
                
                // Additional first-launch tasks can be added here
                // For example: showing tutorial, requesting permissions, etc.
            } else {
                Log.i(TAG, "Not first launch, skipping initial setup")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during first launch setup", e)
        }
    }
    
    /**
     * Check if this is the first launch of the app.
     */
    fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * Get the application-wide coroutine scope.
     */
    fun getApplicationScope(): CoroutineScope = applicationScope
}

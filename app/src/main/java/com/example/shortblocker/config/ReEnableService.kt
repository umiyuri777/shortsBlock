package com.example.shortblocker.config

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service to handle re-enabling the blocker after temporary disable expires
 */
class ReEnableService : Service() {
    
    @Inject
    lateinit var configurationManager: ConfigurationManager
    
    @Inject
    lateinit var temporaryDisableManager: TemporaryDisableManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        // Note: In a real implementation with Hilt/Dagger, injection would happen here
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            try {
                // Clear temporary disable
                configurationManager.clearTemporaryDisable()
                
                // Show re-enabled notification
                temporaryDisableManager.showReEnabledNotification()
            } finally {
                // Stop the service
                stopSelf(startId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

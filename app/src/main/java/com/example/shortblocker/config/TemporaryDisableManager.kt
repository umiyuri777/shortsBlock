package com.example.shortblocker.config

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.shortblocker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages temporary disable timer using AlarmManager
 */
@Singleton
class TemporaryDisableManager @Inject constructor(
    private val context: Context,
    private val configurationManager: ConfigurationManager
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "temporary_disable_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_RE_ENABLE = "com.example.shortblocker.ACTION_RE_ENABLE"
        private const val REQUEST_CODE = 1000
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Schedule temporary disable for specified duration
     * @param durationMinutes Duration in minutes
     */
    suspend fun scheduleTemporaryDisable(durationMinutes: Int) {
        if (durationMinutes <= 0) {
            cancelTemporaryDisable()
            return
        }
        
        // Set the temporary disable in configuration
        configurationManager.setTemporaryDisable(durationMinutes)
        
        // Schedule alarm to re-enable
        val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        scheduleReEnableAlarm(triggerTime)
        
        // Show notification
        showTemporaryDisableNotification(durationMinutes)
    }
    
    /**
     * Cancel temporary disable and re-enable immediately
     */
    suspend fun cancelTemporaryDisable() {
        configurationManager.clearTemporaryDisable()
        cancelReEnableAlarm()
        dismissNotification()
    }
    
    /**
     * Check if currently temporarily disabled and update if expired
     */
    suspend fun checkAndUpdateStatus() {
        val isDisabled = configurationManager.isTemporarilyDisabled()
        if (!isDisabled) {
            // Already expired or not disabled, ensure alarm is cancelled
            cancelReEnableAlarm()
            dismissNotification()
        }
    }
    
    private fun scheduleReEnableAlarm(triggerTime: Long) {
        val intent = Intent(context, ReEnableReceiver::class.java).apply {
            action = ACTION_RE_ENABLE
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Use exact alarm for precise timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    
    private fun cancelReEnableAlarm() {
        val intent = Intent(context, ReEnableReceiver::class.java).apply {
            action = ACTION_RE_ENABLE
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    private fun showTemporaryDisableNotification(durationMinutes: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Short Video Blocker Paused")
            .setContentText("Blocker will resume in $durationMinutes minutes")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporary Disable Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the blocker is temporarily disabled"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification when service is re-enabled
     */
    fun showReEnabledNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Short Video Blocker Resumed")
            .setContentText("Blocker is now active again")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}

/**
 * BroadcastReceiver to handle re-enable alarm
 */
class ReEnableReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.shortblocker.ACTION_RE_ENABLE") {
            scope.launch {
                // Get the configuration manager and clear temporary disable
                // Note: In a real implementation, you'd use dependency injection
                // For now, we'll trigger through the service
                val reEnableIntent = Intent(context, ReEnableService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(reEnableIntent)
                } else {
                    context.startService(reEnableIntent)
                }
            }
        }
    }
}

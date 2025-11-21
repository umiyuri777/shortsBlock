package com.example.shortblocker.error

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.shortblocker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages safe mode activation when consecutive errors occur.
 * Safe mode temporarily disables detection to prevent system instability.
 */
@Singleton
class SafeModeManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SafeModeManager"
        private const val ERROR_THRESHOLD = 5
        private const val COOLDOWN_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val ERROR_RESET_DURATION_MS = 60 * 1000L // 1 minute
        private const val SAFE_MODE_NOTIFICATION_CHANNEL_ID = "safe_mode"
        private const val SAFE_MODE_NOTIFICATION_ID = 1002
    }

    private val consecutiveErrors = AtomicInteger(0)
    private val _isInSafeMode = MutableStateFlow(false)
    val isInSafeMode: StateFlow<Boolean> = _isInSafeMode.asStateFlow()

    private var cooldownJob: Job? = null
    private var errorResetJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        createNotificationChannel()
    }

    /**
     * Record an error occurrence. If threshold is reached, enter safe mode.
     */
    fun recordError() {
        val errorCount = consecutiveErrors.incrementAndGet()
        Log.d(TAG, "Error recorded. Consecutive errors: $errorCount")

        // Reset the error count timer
        scheduleErrorReset()

        if (errorCount >= ERROR_THRESHOLD && !_isInSafeMode.value) {
            enterSafeMode()
        }
    }

    /**
     * Reset the error counter (called when operations succeed).
     */
    fun resetErrors() {
        val previousCount = consecutiveErrors.getAndSet(0)
        if (previousCount > 0) {
            Log.d(TAG, "Error counter reset from $previousCount to 0")
        }
        errorResetJob?.cancel()
        errorResetJob = null
    }

    /**
     * Check if the system is currently in safe mode.
     */
    fun isInSafeModeNow(): Boolean = _isInSafeMode.value

    /**
     * Manually exit safe mode (for testing or user override).
     */
    fun exitSafeMode() {
        if (_isInSafeMode.value) {
            Log.i(TAG, "Manually exiting safe mode")
            _isInSafeMode.value = false
            consecutiveErrors.set(0)
            cooldownJob?.cancel()
            cooldownJob = null
            notifySafeModeExit()
        }
    }

    private fun enterSafeMode() {
        Log.w(TAG, "Entering safe mode due to $ERROR_THRESHOLD consecutive errors")
        _isInSafeMode.value = true
        
        notifySafeModeEntry()
        scheduleCooldown()
    }

    private fun scheduleErrorReset() {
        errorResetJob?.cancel()
        errorResetJob = coroutineScope.launch {
            delay(ERROR_RESET_DURATION_MS)
            if (consecutiveErrors.get() > 0) {
                Log.d(TAG, "Error reset timer expired, resetting counter")
                consecutiveErrors.set(0)
            }
        }
    }

    private fun scheduleCooldown() {
        cooldownJob?.cancel()
        cooldownJob = coroutineScope.launch {
            Log.d(TAG, "Safe mode cooldown started for ${COOLDOWN_DURATION_MS / 1000} seconds")
            delay(COOLDOWN_DURATION_MS)
            
            Log.i(TAG, "Cooldown period completed, exiting safe mode")
            _isInSafeMode.value = false
            consecutiveErrors.set(0)
            notifySafeModeExit()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SAFE_MODE_NOTIFICATION_CHANNEL_ID,
                "Safe Mode Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about safe mode activation and deactivation"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notifySafeModeEntry() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, SAFE_MODE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Safe Mode Activated")
            .setContentText("Short video detection temporarily disabled due to repeated errors. Will resume in ${COOLDOWN_DURATION_MS / 60000} minutes.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "The blocker has detected multiple consecutive errors and entered safe mode to prevent system instability. " +
                "Detection will automatically resume after ${COOLDOWN_DURATION_MS / 60000} minutes."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SAFE_MODE_NOTIFICATION_ID, notification)
    }

    private fun notifySafeModeExit() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, SAFE_MODE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Safe Mode Deactivated")
            .setContentText("Short video detection has been resumed.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SAFE_MODE_NOTIFICATION_ID, notification)
    }
}

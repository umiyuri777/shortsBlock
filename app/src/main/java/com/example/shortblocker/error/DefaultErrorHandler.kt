package com.example.shortblocker.error

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.shortblocker.R
import com.example.shortblocker.data.BlockActionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of ErrorHandler that handles errors with logging and user notifications.
 */
@Singleton
class DefaultErrorHandler @Inject constructor(
    private val context: Context,
    private val safeModeManager: SafeModeManager
) : ErrorHandler {

    companion object {
        private const val TAG = "DefaultErrorHandler"
        private const val ERROR_NOTIFICATION_CHANNEL_ID = "blocker_errors"
        private const val ERROR_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    override fun handleError(error: BlockerError) {
        logError(error)

        when (error) {
            is BlockerError.PermissionError -> {
                // Permission errors should notify the user
                notifyUser(error)
            }
            is BlockerError.DetectionError -> {
                // Detection errors are logged but don't notify user
                // Record error for safe mode tracking
                safeModeManager.recordError()
            }
            is BlockerError.ActionError -> {
                // Action errors are logged and may trigger fallback
                safeModeManager.recordError()
                if (error.canRetry) {
                    tryFallbackAction()
                }
            }
            is BlockerError.SystemError -> {
                // System errors are logged and tracked
                safeModeManager.recordError()
                if (error.cause != null) {
                    Log.e(TAG, "System error: ${error.message}", error.cause)
                }
            }
        }
    }

    override fun logError(error: BlockerError) {
        when (error) {
            is BlockerError.PermissionError -> {
                Log.w(TAG, "Permission error: ${error.permission} - ${error.message}")
            }
            is BlockerError.DetectionError -> {
                Log.e(TAG, "Detection error for ${error.platform}: ${error.context}", error.cause)
            }
            is BlockerError.ActionError -> {
                Log.e(TAG, "Action error for ${error.action}: canRetry=${error.canRetry}", error.cause)
            }
            is BlockerError.SystemError -> {
                Log.e(TAG, "System error: ${error.message}", error.cause)
            }
        }
    }

    override fun notifyUser(error: BlockerError) {
        val (title, message) = when (error) {
            is BlockerError.PermissionError -> {
                "Permission Required" to "Please grant ${error.permission} permission for the blocker to work properly."
            }
            is BlockerError.DetectionError -> {
                "Detection Issue" to "Unable to detect short videos on ${error.platform}. The app may have been updated."
            }
            is BlockerError.ActionError -> {
                "Block Action Failed" to "Unable to execute ${error.action} action. Trying alternative method."
            }
            is BlockerError.SystemError -> {
                "System Error" to error.message
            }
        }

        showNotification(title, message)
    }

    override fun tryFallbackAction() {
        // Fallback logic: if overlay fails, try navigate back
        // This is a simplified implementation - actual fallback would be coordinated with BlockActionManager
        Log.i(TAG, "Attempting fallback action")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ERROR_NOTIFICATION_CHANNEL_ID,
                "Error Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about blocker errors and issues"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, ERROR_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }
}

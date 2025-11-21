package com.example.shortblocker.error

import android.util.Log
import com.example.shortblocker.BuildConfig
import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.BlockLogDao
import com.example.shortblocker.data.BlockLogEntity
import com.example.shortblocker.data.DetectionMethod
import com.example.shortblocker.data.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger for blocker events and errors with database persistence.
 */
@Singleton
class BlockerLogger @Inject constructor(
    private val blockLogDao: BlockLogDao
) {
    companion object {
        private const val TAG = "BlockerLogger"
        private const val MAX_LOG_AGE_DAYS = 30
        private const val LOG_ROTATION_THRESHOLD = 1000
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Log a successful block action.
     */
    fun logBlockAction(
        platform: Platform,
        detectionMethod: DetectionMethod,
        actionTaken: BlockActionType,
        packageName: String
    ) {
        // Debug log
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Block action: platform=$platform, method=$detectionMethod, action=$actionTaken, package=$packageName")
        }

        // Persist to database
        coroutineScope.launch {
            try {
                val logEntity = BlockLogEntity(
                    timestamp = System.currentTimeMillis(),
                    platform = platform.name,
                    detectionMethod = detectionMethod.name,
                    actionTaken = actionTaken.name,
                    packageName = packageName
                )
                blockLogDao.insert(logEntity)

                // Check if rotation is needed
                checkAndRotateLogs()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist block log", e)
            }
        }
    }

    /**
     * Log an error event.
     */
    fun logError(
        error: BlockerError,
        additionalInfo: String? = null
    ) {
        val errorMessage = when (error) {
            is BlockerError.PermissionError -> "Permission error: ${error.permission} - ${error.message}"
            is BlockerError.DetectionError -> "Detection error: ${error.platform} - ${error.context}"
            is BlockerError.ActionError -> "Action error: ${error.action} - ${error.cause.message}"
            is BlockerError.SystemError -> "System error: ${error.message}"
        }

        // Always log errors
        Log.e(TAG, errorMessage + (additionalInfo?.let { " | $it" } ?: ""))

        // In debug mode, log stack trace
        if (BuildConfig.DEBUG) {
            when (error) {
                is BlockerError.DetectionError -> Log.e(TAG, "Stack trace:", error.cause)
                is BlockerError.ActionError -> Log.e(TAG, "Stack trace:", error.cause)
                is BlockerError.SystemError -> error.cause?.let { Log.e(TAG, "Stack trace:", it) }
                else -> {}
            }
        }
    }

    /**
     * Log debug information (only in debug builds).
     */
    fun logDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * Log info message.
     */
    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    /**
     * Log warning message.
     */
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * Get recent block logs for statistics.
     */
    suspend fun getRecentLogs(limit: Int = 100): List<BlockLogEntity> {
        return try {
            blockLogDao.getRecentLogs(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve recent logs", e)
            emptyList()
        }
    }

    /**
     * Get logs for a specific time range.
     */
    suspend fun getLogsByTimeRange(startTime: Long, endTime: Long): List<BlockLogEntity> {
        return try {
            blockLogDao.getLogsByTimeRange(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve logs by time range", e)
            emptyList()
        }
    }

    /**
     * Get logs for a specific platform.
     */
    suspend fun getLogsByPlatform(platform: Platform): List<BlockLogEntity> {
        return try {
            blockLogDao.getLogsByPlatform(platform.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve logs by platform", e)
            emptyList()
        }
    }

    /**
     * Delete all logs (for privacy/cleanup).
     */
    suspend fun clearAllLogs() {
        try {
            blockLogDao.deleteAll()
            Log.i(TAG, "All logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }

    /**
     * Check log count and rotate if necessary.
     */
    private suspend fun checkAndRotateLogs() {
        try {
            val logCount = blockLogDao.getLogCount()
            
            if (logCount > LOG_ROTATION_THRESHOLD) {
                rotateLogs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check log count", e)
        }
    }

    /**
     * Rotate logs by deleting old entries.
     */
    private suspend fun rotateLogs() {
        try {
            val cutoffTime = System.currentTimeMillis() - (MAX_LOG_AGE_DAYS * 24 * 60 * 60 * 1000L)
            val deletedCount = blockLogDao.deleteOldLogs(cutoffTime)
            
            Log.i(TAG, "Log rotation completed. Deleted $deletedCount old logs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate logs", e)
        }
    }

    /**
     * Manually trigger log rotation.
     */
    suspend fun performLogRotation() {
        rotateLogs()
    }
}

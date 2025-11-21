package com.example.shortblocker.error

import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.Platform

/**
 * Sealed class representing different types of errors that can occur in the blocker system.
 */
sealed class BlockerError {
    /**
     * Error related to missing or denied permissions.
     */
    data class PermissionError(
        val permission: String,
        val message: String
    ) : BlockerError()

    /**
     * Error that occurred during platform detection.
     */
    data class DetectionError(
        val platform: Platform,
        val cause: Throwable,
        val context: String? = null
    ) : BlockerError()

    /**
     * Error that occurred while executing a block action.
     */
    data class ActionError(
        val action: BlockActionType,
        val cause: Throwable,
        val canRetry: Boolean = true
    ) : BlockerError()

    /**
     * General system error.
     */
    data class SystemError(
        val message: String,
        val cause: Throwable? = null
    ) : BlockerError()
}

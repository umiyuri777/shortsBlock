package com.example.shortblocker.error

import android.content.Context
import android.util.Log
import com.example.shortblocker.data.BlockActionType

/**
 * Interface for handling errors in the blocker system.
 */
interface ErrorHandler {
    /**
     * Handle an error by determining the appropriate response.
     */
    fun handleError(error: BlockerError)

    /**
     * Log an error for debugging purposes.
     */
    fun logError(error: BlockerError)

    /**
     * Notify the user about an error if necessary.
     */
    fun notifyUser(error: BlockerError)

    /**
     * Try a fallback action when the primary action fails.
     */
    fun tryFallbackAction()
}

package com.example.shortblocker.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Debounces rapid accessibility events to optimize performance.
 * 
 * This class prevents processing of duplicate or rapid-fire events by
 * enforcing a minimum time interval between processed events.
 * 
 * Requirements: 6.1, 6.2 - Performance optimization
 */
class EventDebouncer(
    private val delayMs: Long = 100L
) {
    companion object {
        private const val TAG = "EventDebouncer"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val lastEventTime = AtomicLong(0L)
    private var pendingRunnable: Runnable? = null
    
    /**
     * Debounce an action. Only executes if enough time has passed since last execution.
     * 
     * @param key Unique key for this debounce operation (e.g., package name)
     * @param action The action to execute after debounce delay
     */
    fun debounce(key: String, action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastEventTime.get()
        
        // Cancel any pending action
        pendingRunnable?.let { handler.removeCallbacks(it) }
        
        // Calculate time since last event
        val timeSinceLastEvent = currentTime - lastTime
        
        if (timeSinceLastEvent >= delayMs) {
            // Enough time has passed, execute immediately
            lastEventTime.set(currentTime)
            action()
            
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Executed immediately for key: $key")
            }
        } else {
            // Schedule execution after remaining delay
            val remainingDelay = delayMs - timeSinceLastEvent
            
            pendingRunnable = Runnable {
                lastEventTime.set(System.currentTimeMillis())
                action()
                
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Executed after delay for key: $key")
                }
            }
            
            handler.postDelayed(pendingRunnable!!, remainingDelay)
            
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Debounced event for key: $key, delay: ${remainingDelay}ms")
            }
        }
    }
    
    /**
     * Cancel any pending debounced actions.
     */
    fun cancel() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
    }
    
    /**
     * Reset the debouncer state.
     */
    fun reset() {
        cancel()
        lastEventTime.set(0L)
    }
}

/**
 * Package-specific event debouncer that maintains separate debounce state
 * for each package to avoid cross-package interference.
 * 
 * Requirements: 6.1, 6.2 - Performance optimization with per-package tracking
 */
class PackageEventDebouncer(
    private val delayMs: Long = 100L
) {
    companion object {
        private const val TAG = "PackageEventDebouncer"
    }
    
    private val debouncers = mutableMapOf<String, EventDebouncer>()
    
    /**
     * Debounce an action for a specific package.
     * Each package has its own debounce state.
     * 
     * @param packageName The package name to debounce for
     * @param action The action to execute after debounce delay
     */
    fun debounce(packageName: String, action: () -> Unit) {
        val debouncer = debouncers.getOrPut(packageName) {
            EventDebouncer(delayMs)
        }
        
        debouncer.debounce(packageName, action)
    }
    
    /**
     * Cancel debounced actions for a specific package.
     */
    fun cancel(packageName: String) {
        debouncers[packageName]?.cancel()
    }
    
    /**
     * Cancel all debounced actions.
     */
    fun cancelAll() {
        debouncers.values.forEach { it.cancel() }
    }
    
    /**
     * Reset debouncer for a specific package.
     */
    fun reset(packageName: String) {
        debouncers[packageName]?.reset()
        debouncers.remove(packageName)
    }
    
    /**
     * Reset all debouncers.
     */
    fun resetAll() {
        debouncers.values.forEach { it.reset() }
        debouncers.clear()
    }
}

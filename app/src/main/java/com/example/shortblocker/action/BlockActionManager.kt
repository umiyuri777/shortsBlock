package com.example.shortblocker.action

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ブロックアクションを管理し、適切なアクションを実行するマネージャー
 */
class BlockActionManager(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val settingsRepository: SettingsRepository
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val overlayAction: OverlayBlockAction by lazy {
        OverlayBlockAction(context, accessibilityService, settingsRepository)
    }
    
    private val navigateBackAction: NavigateBackAction by lazy {
        NavigateBackAction(context, accessibilityService)
    }
    
    private val notificationAction: NotificationAction by lazy {
        NotificationAction(context)
    }
    
    companion object {
        private const val TAG = "BlockActionManager"
    }
    
    /**
     * 検出結果に基づいてブロックアクションを実行
     */
    fun executeBlockAction(result: DetectionResult, appContext: AppContext) {
        scope.launch {
            try {
                val settings = settingsRepository.getSettings()
                val actionType = settings.blockActionType
                
                Log.d(TAG, "Executing block action: $actionType for platform: ${result.platform}")
                
                when (actionType) {
                    BlockActionType.OVERLAY -> {
                        executeWithFallback(overlayAction, result, appContext)
                    }
                    
                    BlockActionType.NAVIGATE_BACK -> {
                        executeWithFallback(navigateBackAction, result, appContext)
                    }
                    
                    BlockActionType.NOTIFICATION -> {
                        executeWithFallback(notificationAction, result, appContext)
                    }
                    
                    BlockActionType.COMBINED -> {
                        // オーバーレイ + 通知の複合アクション
                        executeCombinedAction(result, appContext)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error executing block action", e)
                handleError(e, result, appContext)
            }
        }
    }
    
    /**
     * フォールバック付きでアクションを実行
     */
    private suspend fun executeWithFallback(
        action: BlockAction,
        result: DetectionResult,
        appContext: AppContext
    ) {
        try {
            if (action.canExecute()) {
                action.execute(result, appContext)
            } else {
                Log.w(TAG, "Action cannot execute, falling back to navigate back")
                fallbackToNavigateBack(result, appContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action execution failed, falling back", e)
            fallbackToNavigateBack(result, appContext)
        }
    }
    
    /**
     * 複合アクション（オーバーレイ + 通知）を実行
     */
    private suspend fun executeCombinedAction(
        result: DetectionResult,
        appContext: AppContext
    ) {
        try {
            // まずオーバーレイを試みる
            if (overlayAction.canExecute()) {
                overlayAction.execute(result, appContext)
            } else {
                // オーバーレイが使えない場合は戻るアクション
                navigateBackAction.execute(result, appContext)
            }
            
            // 通知も表示
            if (notificationAction.canExecute()) {
                notificationAction.execute(result, appContext)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Combined action failed", e)
            // フォールバック
            fallbackToNavigateBack(result, appContext)
        }
    }
    
    /**
     * エラー時のフォールバック処理
     */
    private suspend fun fallbackToNavigateBack(
        result: DetectionResult,
        appContext: AppContext
    ) {
        try {
            navigateBackAction.execute(result, appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback action also failed", e)
            // 最後の手段として通知のみ
            try {
                notificationAction.execute(result, appContext)
            } catch (e2: Exception) {
                Log.e(TAG, "All actions failed", e2)
            }
        }
    }
    
    /**
     * エラーハンドリング
     */
    private suspend fun handleError(
        error: Exception,
        result: DetectionResult,
        appContext: AppContext
    ) {
        Log.e(TAG, "Handling error for platform: ${result.platform}", error)
        
        // エラーの種類に応じた処理
        when (error) {
            is SecurityException -> {
                // 権限エラー - 通知で知らせる
                Log.e(TAG, "Permission error, notifying user")
                try {
                    notificationAction.execute(result, appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to notify user about permission error", e)
                }
            }
            
            else -> {
                // その他のエラー - フォールバック
                fallbackToNavigateBack(result, appContext)
            }
        }
    }
    
    /**
     * クリーンアップ処理
     */
    fun cleanup() {
        try {
            overlayAction.cleanup()
            navigateBackAction.cleanup()
            notificationAction.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

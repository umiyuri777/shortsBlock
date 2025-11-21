package com.example.shortblocker.action

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.example.shortblocker.R
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * オーバーレイを表示してショート動画をブロックするアクション
 */
class OverlayBlockAction(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val settingsRepository: SettingsRepository
) : BlockAction {
    
    private val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null
    
    companion object {
        private const val AUTO_DISMISS_DELAY_MS = 10000L // 10秒後に自動消去
    }
    
    override suspend fun execute(result: DetectionResult, context: AppContext) {
        withContext(Dispatchers.Main) {
            if (overlayView != null) {
                // すでにオーバーレイが表示されている場合は何もしない
                return@withContext
            }
            
            showOverlay(result)
        }
    }
    
    override fun canExecute(): Boolean {
        // オーバーレイ権限があるかチェック
        return android.provider.Settings.canDrawOverlays(context)
    }
    
    private fun showOverlay(result: DetectionResult) {
        try {
            // レイアウトをインフレート
            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(R.layout.overlay_block, null)
            
            // ウィンドウパラメータを設定
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            // ボタンのクリックリスナーを設定
            overlayView?.findViewById<Button>(R.id.btnGoBack)?.setOnClickListener {
                performGoBack()
                dismissOverlay()
            }
            
            overlayView?.findViewById<Button>(R.id.btnDisable30Min)?.setOnClickListener {
                disableTemporarily()
                dismissOverlay()
            }
            
            // オーバーレイを表示
            windowManager.addView(overlayView, params)
            
            // 自動消去タイマーをスケジュール
            scheduleAutoDismiss()
            
        } catch (e: Exception) {
            // エラーが発生した場合はクリーンアップ
            overlayView = null
            throw e
        }
    }
    
    private fun scheduleAutoDismiss() {
        autoDismissRunnable = Runnable {
            dismissOverlay()
        }
        handler.postDelayed(autoDismissRunnable!!, AUTO_DISMISS_DELAY_MS)
    }
    
    private fun dismissOverlay() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
            }
            
            // タイマーをキャンセル
            autoDismissRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
                autoDismissRunnable = null
            }
        } catch (e: Exception) {
            // ビューが既に削除されている場合は無視
            overlayView = null
        }
    }
    
    private fun performGoBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    private fun disableTemporarily() {
        // 30分間一時的に無効化
        // この実装はSettingsRepositoryを通じて行う
        // 実際の実装はConfigurationManagerで行うべきだが、
        // ここでは簡易的に実装
        // TODO: ConfigurationManagerと統合
    }
    
    override fun cleanup() {
        dismissOverlay()
    }
}

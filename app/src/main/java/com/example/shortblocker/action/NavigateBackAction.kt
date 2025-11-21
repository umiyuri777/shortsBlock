package com.example.shortblocker.action

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.shortblocker.R
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 前の画面に戻ることでショート動画をブロックするアクション
 */
class NavigateBackAction(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) : BlockAction {
    
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "short_blocker_navigate_back"
        private const val CHANNEL_NAME = "Short Video Blocks"
        private const val NOTIFICATION_ID = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    override suspend fun execute(result: DetectionResult, context: AppContext) {
        withContext(Dispatchers.Main) {
            // 前の画面に戻る
            performNavigateBack()
            
            // 簡易通知を表示
            showBriefNotification(result.platform)
        }
    }
    
    override fun canExecute(): Boolean {
        // このアクションは常に実行可能
        return true
    }
    
    private fun performNavigateBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    private fun showBriefNotification(platform: Platform) {
        val platformName = when (platform) {
            Platform.YOUTUBE -> "YouTube Shorts"
            Platform.INSTAGRAM -> "Instagram Reels"
            Platform.TIKTOK -> "TikTok"
            Platform.UNKNOWN -> "Short Video"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Short Video Blocked")
            .setContentText("$platformName blocked - navigated back")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // 3秒後に自動消去
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications when short videos are blocked"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun cleanup() {
        // クリーンアップ処理は特に必要なし
    }
}

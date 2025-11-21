package com.example.shortblocker.action

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.shortblocker.R
import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionResult
import com.example.shortblocker.data.Platform
import com.example.shortblocker.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通知を表示してショート動画のブロックを知らせるアクション
 */
class NotificationAction(
    private val context: Context
) : BlockAction {
    
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "short_blocker_notifications"
        private const val CHANNEL_NAME = "Short Video Blocks"
        private const val NOTIFICATION_ID_BASE = 2000
    }
    
    private var notificationIdCounter = NOTIFICATION_ID_BASE
    
    init {
        createNotificationChannel()
    }
    
    override suspend fun execute(result: DetectionResult, context: AppContext) {
        withContext(Dispatchers.Main) {
            showNotification(result)
        }
    }
    
    override fun canExecute(): Boolean {
        // Android 13以降では通知権限が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 権限チェックは呼び出し側で行うべきだが、ここでは簡易的に常にtrueを返す
            // 実際の実装では権限チェックを追加すべき
        }
        return true
    }
    
    private fun showNotification(result: DetectionResult) {
        val platformName = getPlatformName(result.platform)
        val detectionMethodText = getDetectionMethodText(result.detectionMethod.name)
        
        // メインアクティビティを開くインテント
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 通知を構築
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚫 Short Video Blocked")
            .setContentText("$platformName was blocked")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$platformName was blocked\nDetection: $detectionMethodText\nYou can change settings in the app.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        // 通知を表示（ユニークなIDを使用）
        notificationManager.notify(getNextNotificationId(), notification)
    }
    
    private fun getPlatformName(platform: Platform): String {
        return when (platform) {
            Platform.YOUTUBE -> "YouTube Shorts"
            Platform.INSTAGRAM -> "Instagram Reels"
            Platform.TIKTOK -> "TikTok"
            Platform.UNKNOWN -> "Short Video"
        }
    }
    
    private fun getDetectionMethodText(method: String): String {
        return when (method) {
            "UI_ELEMENT" -> "UI Element"
            "URL_PATTERN" -> "URL Pattern"
            "ACTIVITY_NAME" -> "Activity Name"
            "HEURISTIC" -> "Heuristic"
            else -> method
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when short videos are blocked"
                setShowBadge(true)
                enableLights(true)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun getNextNotificationId(): Int {
        notificationIdCounter++
        if (notificationIdCounter > NOTIFICATION_ID_BASE + 100) {
            notificationIdCounter = NOTIFICATION_ID_BASE
        }
        return notificationIdCounter
    }
    
    override fun cleanup() {
        // 必要に応じて全ての通知をキャンセル
        // notificationManager.cancelAll()
    }
}

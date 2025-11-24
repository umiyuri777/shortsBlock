package com.example.shortblocker.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 権限管理のヘルパークラス
 */
class PermissionHelper(private val activity: Activity) {

    /**
     * すべての必要な権限がチェックされているか確認
     */
    fun checkAllPermissions(): PermissionStatus {
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(),
            overlayGranted = isOverlayPermissionGranted(),
            notificationGranted = isNotificationPermissionGranted()
        )
    }

    /**
     * AccessibilityServiceが有効かチェック
     * 
     * 以下の2つの条件を確認します：
     * 1. Accessibility Service全体が有効になっているか
     * 2. このアプリのAccessibility Serviceが有効になっているか
     * 
     * 複数の方法で確認し、いずれかで有効と判定されればtrueを返します。
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        // 1. Accessibility Service全体が有効かチェック
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                activity.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
        
        if (!accessibilityEnabled) {
            return false
        }
        
        // 2. このアプリのサービスが有効になっているかチェック
        // 方法1: AccessibilityManagerを使用（最も確実）
        val accessibilityManager = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServicesList = accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ) ?: emptyList()
        
        // サービスIDの複数の形式をチェック
        val serviceIdVariants = listOf(
            "${activity.packageName}/${activity.packageName}.service.BlockerAccessibilityService",
            "${activity.packageName}/.service.BlockerAccessibilityService",
            "${activity.packageName}:${activity.packageName}.service.BlockerAccessibilityService",
            "${activity.packageName}:.service.BlockerAccessibilityService"
        )
        
        // AccessibilityManagerで確認
        val foundInManager = enabledServicesList.any { serviceInfo ->
            serviceIdVariants.any { serviceId ->
                serviceInfo.id == serviceId || serviceInfo.id.endsWith(serviceId)
            } || serviceInfo.id.contains("BlockerAccessibilityService")
        }
        
        if (foundInManager) {
            return true
        }
        
        // 方法2: Settings.Secureを使用（フォールバック）
        val enabledServices = try {
            Settings.Secure.getString(
                activity.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
        } catch (e: Exception) {
            ""
        }
        
        // サービス名の複数の形式に対応（コロン区切り、スラッシュ区切りなど）
        val serviceNameVariants = listOf(
            "${activity.packageName}/.service.BlockerAccessibilityService",
            "${activity.packageName}/${activity.packageName}.service.BlockerAccessibilityService",
            "${activity.packageName}:.service.BlockerAccessibilityService",
            "${activity.packageName}:${activity.packageName}.service.BlockerAccessibilityService"
        )
        
        // いずれかの形式で一致するかチェック
        return serviceNameVariants.any { variant ->
            enabledServices.contains(variant)
        }
    }

    /**
     * オーバーレイ権限が付与されているかチェック
     */
    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true
        }
    }

    /**
     * 通知権限が付与されているかチェック
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(activity).areNotificationsEnabled()
        } else {
            true
        }
    }

    /**
     * AccessibilityService設定画面を開く
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivity(intent)
    }

    /**
     * オーバーレイ権限設定画面を開く
     */
    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        }
    }

    /**
     * 通知権限をリクエスト（Android 13+）
     */
    fun requestNotificationPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * 権限リクエストガイドを表示
     */
    fun showPermissionGuide(onComplete: () -> Unit) {
        val status = checkAllPermissions()
        
        when {
            !status.accessibilityEnabled -> {
                showAccessibilityGuide(onComplete)
            }
            !status.overlayGranted -> {
                showOverlayGuide(onComplete)
            }
            !status.notificationGranted -> {
                showNotificationGuide(onComplete)
            }
            else -> {
                onComplete()
            }
        }
    }

    private fun showAccessibilityGuide(onComplete: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Accessibility Service Required")
            .setMessage(
                "This app needs accessibility service permission to detect short videos.\n\n" +
                "Steps:\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Find 'Short Video Blocker' in the list\n" +
                "3. Toggle it ON\n" +
                "4. Confirm the permission dialog"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAccessibilitySettings()
                onComplete()
            }
            .setNegativeButton("Skip") { _, _ ->
                onComplete()
            }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayGuide(onComplete: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Overlay Permission Required")
            .setMessage(
                "This app needs overlay permission to display block messages.\n\n" +
                "Steps:\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Toggle 'Allow display over other apps' ON"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openOverlaySettings()
                onComplete()
            }
            .setNegativeButton("Skip") { _, _ ->
                onComplete()
            }
            .setCancelable(false)
            .show()
    }

    private fun showNotificationGuide(onComplete: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("Notification Permission Required")
                .setMessage(
                    "This app needs notification permission to alert you when short videos are blocked.\n\n" +
                    "Please allow notifications in the next dialog."
                )
                .setPositiveButton("Continue") { _, _ ->
                    onComplete()
                }
                .setNegativeButton("Skip") { _, _ ->
                    onComplete()
                }
                .setCancelable(false)
                .show()
        } else {
            onComplete()
        }
    }

    /**
     * 初回起動時の権限セットアップウィザードを表示
     */
    fun showSetupWizard(onComplete: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Welcome to Short Video Blocker")
            .setMessage(
                "This app helps you block short-form video content like YouTube Shorts, Instagram Reels, and TikTok.\n\n" +
                "To work properly, we need to set up a few permissions. This will only take a minute."
            )
            .setPositiveButton("Get Started") { _, _ ->
                showPermissionGuide(onComplete)
            }
            .setNegativeButton("Later") { _, _ ->
                onComplete()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 権限が不足している場合の警告を表示
     */
    fun showMissingPermissionsWarning() {
        val status = checkAllPermissions()
        val missingPermissions = mutableListOf<String>()

        if (!status.accessibilityEnabled) {
            missingPermissions.add("• Accessibility Service")
        }
        if (!status.overlayGranted) {
            missingPermissions.add("• Overlay Permission")
        }
        if (!status.notificationGranted) {
            missingPermissions.add("• Notification Permission")
        }

        if (missingPermissions.isNotEmpty()) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("Missing Permissions")
                .setMessage(
                    "The following permissions are required for the app to work:\n\n" +
                    missingPermissions.joinToString("\n") +
                    "\n\nPlease grant these permissions in Settings."
                )
                .setPositiveButton("Open Settings") { _, _ ->
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    data class PermissionStatus(
        val accessibilityEnabled: Boolean,
        val overlayGranted: Boolean,
        val notificationGranted: Boolean
    ) {
        val allGranted: Boolean
            get() = accessibilityEnabled && overlayGranted && notificationGranted
    }
}

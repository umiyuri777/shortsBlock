package com.example.shortblocker.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.shortblocker.R
import com.example.shortblocker.data.BlockLogDao
import com.example.shortblocker.data.SettingsRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var blockLogDao: BlockLogDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var accessibilityStatusText: TextView
    private lateinit var overlayStatusText: TextView
    private lateinit var notificationStatusText: TextView
    private lateinit var versionText: TextView
    private lateinit var totalBlocksText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        initializeViews()
        setupListeners()
        updatePermissionStatus()
        loadAppInfo()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initializeViews() {
        accessibilityStatusText = findViewById(R.id.accessibilityStatusText)
        overlayStatusText = findViewById(R.id.overlayStatusText)
        notificationStatusText = findViewById(R.id.notificationStatusText)
        versionText = findViewById(R.id.versionText)
        totalBlocksText = findViewById(R.id.totalBlocksText)
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.accessibilityButton).setOnClickListener {
            openAccessibilitySettings()
        }

        findViewById<MaterialButton>(R.id.overlayButton).setOnClickListener {
            openOverlaySettings()
        }

        findViewById<MaterialButton>(R.id.notificationButton).setOnClickListener {
            openNotificationSettings()
        }

        findViewById<MaterialButton>(R.id.clearLogsButton).setOnClickListener {
            showClearLogsDialog()
        }

        findViewById<MaterialButton>(R.id.resetSettingsButton).setOnClickListener {
            showResetSettingsDialog()
        }
    }

    private fun updatePermissionStatus() {
        // Accessibility Service
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        accessibilityStatusText.text = if (accessibilityEnabled) {
            getString(R.string.enabled)
        } else {
            getString(R.string.disabled)
        }
        accessibilityStatusText.setTextColor(
            getColor(if (accessibilityEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )

        // Overlay Permission
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        overlayStatusText.text = if (overlayGranted) {
            getString(R.string.granted)
        } else {
            getString(R.string.not_granted)
        }
        overlayStatusText.setTextColor(
            getColor(if (overlayGranted) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )

        // Notification Permission
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        } else {
            true
        }
        notificationStatusText.text = if (notificationGranted) {
            getString(R.string.granted)
        } else {
            getString(R.string.not_granted)
        }
        notificationStatusText.setTextColor(
            getColor(if (notificationGranted) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
    }

    private fun loadAppInfo() {
        // Version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            versionText.text = "Unknown"
        }

        // Total blocks
        lifecycleScope.launch {
            val totalBlocks = blockLogDao.getLogCount()
            totalBlocksText.text = totalBlocks.toString()
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        startActivity(intent)
    }

    private fun showClearLogsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_logs)
            .setMessage(R.string.clear_logs_confirmation)
            .setPositiveButton(R.string.clear) { _, _ ->
                clearLogs()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showResetSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_settings)
            .setMessage(R.string.reset_settings_confirmation)
            .setPositiveButton(R.string.reset) { _, _ ->
                resetSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun clearLogs() {
        lifecycleScope.launch {
            blockLogDao.deleteAll()
            totalBlocksText.text = "0"
            MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle(R.string.success)
                .setMessage(R.string.logs_cleared)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun resetSettings() {
        lifecycleScope.launch {
            settingsRepository.resetToDefaults()
            MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle(R.string.success)
                .setMessage(R.string.settings_reset)
                .setPositiveButton(R.string.ok) { _, _ ->
                    finish()
                }
                .show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        // 1. Accessibility Service全体が有効かチェック
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
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
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServicesList = accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ) ?: emptyList()
        
        // サービスIDの複数の形式をチェック
        val serviceIdVariants = listOf(
            "${packageName}/${packageName}.service.BlockerAccessibilityService",
            "${packageName}/.service.BlockerAccessibilityService",
            "${packageName}:${packageName}.service.BlockerAccessibilityService",
            "${packageName}:.service.BlockerAccessibilityService"
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
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
        } catch (e: Exception) {
            ""
        }
        
        // サービス名の複数の形式に対応（コロン区切り、スラッシュ区切りなど）
        val serviceNameVariants = listOf(
            "${packageName}/.service.BlockerAccessibilityService",
            "${packageName}/${packageName}.service.BlockerAccessibilityService",
            "${packageName}:.service.BlockerAccessibilityService",
            "${packageName}:${packageName}.service.BlockerAccessibilityService"
        )
        
        // いずれかの形式で一致するかチェック
        return serviceNameVariants.any { variant ->
            enabledServices.contains(variant)
        }
    }
}

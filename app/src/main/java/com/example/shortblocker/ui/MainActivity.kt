package com.example.shortblocker.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.shortblocker.R
import com.example.shortblocker.config.ConfigurationManager
import com.example.shortblocker.data.BlockActionType
import com.example.shortblocker.data.BlockLogDao
import com.example.shortblocker.data.Platform
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var configurationManager: ConfigurationManager

    @Inject
    lateinit var blockLogDao: BlockLogDao

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var permissionHelper: PermissionHelper
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // User denied notification permission
        }
    }

    private lateinit var serviceToggle: SwitchMaterial
    private lateinit var youtubeCheckbox: MaterialCheckBox
    private lateinit var instagramCheckbox: MaterialCheckBox
    private lateinit var tiktokCheckbox: MaterialCheckBox
    private lateinit var disableButton: MaterialButton
    private lateinit var disableStatusText: TextView
    private lateinit var todayBlocksText: TextView
    private lateinit var weekBlocksText: TextView
    private lateinit var settingsButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionHelper = PermissionHelper(this)

        initializeViews()
        setupListeners()
        loadSettings()
        updateStatistics()
        
        // 初回起動時のセットアップウィザード
        if (isFirstLaunch()) {
            permissionHelper.showSetupWizard {
                // セットアップ完了後、通知権限をリクエスト
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionHelper.requestNotificationPermission(notificationPermissionLauncher)
                }
            }
            markFirstLaunchComplete()
        } else {
            checkPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        updateStatistics()
        updateDisableStatus()
        checkPermissions()
    }

    private fun initializeViews() {
        serviceToggle = findViewById(R.id.serviceToggle)
        youtubeCheckbox = findViewById(R.id.youtubeCheckbox)
        instagramCheckbox = findViewById(R.id.instagramCheckbox)
        tiktokCheckbox = findViewById(R.id.tiktokCheckbox)
        disableButton = findViewById(R.id.disableButton)
        disableStatusText = findViewById(R.id.disableStatusText)
        todayBlocksText = findViewById(R.id.todayBlocksText)
        weekBlocksText = findViewById(R.id.weekBlocksText)
        settingsButton = findViewById(R.id.settingsButton)
    }

    private fun setupListeners() {
        serviceToggle.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                configurationManager.setEnabled(isChecked)
                if (isChecked) {
                    val status = permissionHelper.checkAllPermissions()
                    if (!status.allGranted) {
                        permissionHelper.showPermissionGuide {
                            // 権限ガイド完了後、通知権限をリクエスト
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                                !status.notificationGranted) {
                                permissionHelper.requestNotificationPermission(notificationPermissionLauncher)
                            }
                        }
                    }
                }
            }
        }

        youtubeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                configurationManager.setPlatformEnabled(Platform.YOUTUBE, isChecked)
            }
        }

        instagramCheckbox.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                configurationManager.setPlatformEnabled(Platform.INSTAGRAM, isChecked)
            }
        }

        tiktokCheckbox.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                configurationManager.setPlatformEnabled(Platform.TIKTOK, isChecked)
            }
        }

        findViewById<RadioButton>(R.id.overlayRadio).setOnClickListener {
            lifecycleScope.launch {
                configurationManager.setBlockActionType(BlockActionType.OVERLAY)
            }
        }

        findViewById<RadioButton>(R.id.navigateBackRadio).setOnClickListener {
            lifecycleScope.launch {
                configurationManager.setBlockActionType(BlockActionType.NAVIGATE_BACK)
            }
        }

        findViewById<RadioButton>(R.id.notificationRadio).setOnClickListener {
            lifecycleScope.launch {
                configurationManager.setBlockActionType(BlockActionType.NOTIFICATION)
            }
        }

        findViewById<RadioButton>(R.id.combinedRadio).setOnClickListener {
            lifecycleScope.launch {
                configurationManager.setBlockActionType(BlockActionType.COMBINED)
            }
        }

        disableButton.setOnClickListener {
            lifecycleScope.launch {
                configurationManager.setTemporaryDisable(30)
                updateDisableStatus()
            }
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val settings = configurationManager.getSettings().first()

            serviceToggle.isChecked = settings.isEnabled

            youtubeCheckbox.isChecked = settings.enabledPlatforms.contains(Platform.YOUTUBE)
            instagramCheckbox.isChecked = settings.enabledPlatforms.contains(Platform.INSTAGRAM)
            tiktokCheckbox.isChecked = settings.enabledPlatforms.contains(Platform.TIKTOK)

            when (settings.blockActionType) {
                BlockActionType.OVERLAY -> findViewById<RadioButton>(R.id.overlayRadio).isChecked = true
                BlockActionType.NAVIGATE_BACK -> findViewById<RadioButton>(R.id.navigateBackRadio).isChecked = true
                BlockActionType.NOTIFICATION -> findViewById<RadioButton>(R.id.notificationRadio).isChecked = true
                BlockActionType.COMBINED -> findViewById<RadioButton>(R.id.combinedRadio).isChecked = true
            }
        }
    }

    private fun updateStatistics() {
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val todayStart = getStartOfDay(now)
            val weekStart = getStartOfWeek(now)

            val todayBlocks = blockLogDao.getBlockCountSince(todayStart)
            val weekBlocks = blockLogDao.getBlockCountSince(weekStart)

            todayBlocksText.text = getString(R.string.blocks_count, todayBlocks)
            weekBlocksText.text = getString(R.string.blocks_count, weekBlocks)
        }
    }

    private fun updateDisableStatus() {
        lifecycleScope.launch {
            val endTime = configurationManager.getTemporaryDisableEndTime()
            if (endTime != null && endTime > System.currentTimeMillis()) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val endTimeStr = timeFormat.format(Date(endTime))
                disableStatusText.text = getString(R.string.disabled_until, endTimeStr)
                disableStatusText.visibility = View.VISIBLE
                disableButton.isEnabled = false
            } else {
                disableStatusText.visibility = View.GONE
                disableButton.isEnabled = true
            }
        }
    }

    private fun checkPermissions() {
        val status = permissionHelper.checkAllPermissions()
        
        // サービスが有効だが権限が不足している場合は警告
        lifecycleScope.launch {
            val settings = configurationManager.getSettings().first()
            if (settings.isEnabled && !status.allGranted) {
                permissionHelper.showMissingPermissionsWarning()
            }
        }
    }

    private fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("first_launch", true)
    }

    private fun markFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean("first_launch", false).apply()
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfWeek(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

# Design Document

## Overview

このドキュメントは、Androidでショート動画コンテンツを選択的にブロックするアプリケーションの技術設計を定義します。システムはAccessibilityServiceを中心に構築され、画面要素を監視してショート動画セクションを検出し、適切なブロックアクションを実行します。

### 主要な設計目標

- **選択的ブロック**: アプリ全体ではなく、特定のセクション（Shorts、Reels）のみをブロック
- **拡張性**: 新しいプラットフォームを簡単に追加できる設計
- **パフォーマンス**: 低CPU・低バッテリー消費
- **ユーザビリティ**: 直感的な設定とフィードバック

## Architecture

### システムアーキテクチャ図

```
┌─────────────────────────────────────────────────────────┐
│                    Android System                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   YouTube    │  │  Instagram   │  │   TikTok     │  │
│  │     App      │  │     App      │  │     App      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                  │                  │          │
│         └──────────────────┴──────────────────┘          │
│                            │                             │
│                  Accessibility Events                    │
│                            │                             │
└────────────────────────────┼─────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────┐
│              Short Video Blocker App                     │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │       BlockerAccessibilityService                  │ │
│  │  ┌──────────────────────────────────────────────┐ │ │
│  │  │         Event Processor                      │ │ │
│  │  └──────────────────┬───────────────────────────┘ │ │
│  └───────────────────────┼──────────────────────────── │
│                          │                              │
│  ┌───────────────────────▼──────────────────────────┐  │
│  │         Platform Detector Manager               │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐      │  │
│  │  │ YouTube  │  │Instagram │  │ TikTok   │      │  │
│  │  │ Detector │  │ Detector │  │ Detector │      │  │
│  │  └──────────┘  └──────────┘  └──────────┘      │  │
│  └───────────────────────┬──────────────────────────┘  │
│                          │                              │
│  ┌───────────────────────▼──────────────────────────┐  │
│  │           Block Action Manager                   │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐      │  │
│  │  │ Overlay  │  │ Navigate │  │ Notify   │      │  │
│  │  │ Action   │  │ Back     │  │ Action   │      │  │
│  │  └──────────┘  └──────────┘  └──────────┘      │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Settings & Configuration                 │  │
│  │  - Platform Enable/Disable                       │  │
│  │  - Block Action Type                             │  │
│  │  - Temporary Disable Timer                       │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Local Storage (Room DB)                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### レイヤー構成

1. **Accessibility Layer**: AccessibilityServiceでシステムイベントを受信
2. **Detection Layer**: プラットフォーム固有のロジックでショート動画を検出
3. **Action Layer**: ブロックアクションを実行
4. **Configuration Layer**: ユーザー設定を管理
5. **Storage Layer**: 設定とログをローカルに保存


## Components and Interfaces

### 1. BlockerAccessibilityService

AccessibilityServiceを継承したメインサービス。

```kotlin
class BlockerAccessibilityService : AccessibilityService() {
    private lateinit var eventProcessor: AccessibilityEventProcessor
    private lateinit var detectorManager: PlatformDetectorManager
    private lateinit var actionManager: BlockActionManager
    
    override fun onAccessibilityEvent(event: AccessibilityEvent)
    override fun onInterrupt()
    override fun onServiceConnected()
}
```

**責務:**
- Accessibilityイベントの受信
- イベントの前処理とフィルタリング
- 適切なコンポーネントへのディスパッチ

### 2. AccessibilityEventProcessor

イベントを解析し、現在のアプリコンテキストを判定。

```kotlin
interface AccessibilityEventProcessor {
    fun processEvent(event: AccessibilityEvent): AppContext
    fun extractNodeInfo(event: AccessibilityEvent): List<AccessibilityNodeInfo>
}

data class AppContext(
    val packageName: String,
    val activityName: String?,
    val nodeTree: List<AccessibilityNodeInfo>,
    val timestamp: Long
)
```

**責務:**
- イベントからアプリ情報を抽出
- AccessibilityNodeInfoツリーの構築
- 不要なイベントのフィルタリング

### 3. PlatformDetectorManager

プラットフォーム固有の検出ロジックを管理。

```kotlin
interface PlatformDetectorManager {
    fun detectShortVideo(context: AppContext): DetectionResult
    fun registerDetector(detector: PlatformDetector)
    fun getEnabledPlatforms(): List<String>
}

data class DetectionResult(
    val isShortVideo: Boolean,
    val platform: Platform,
    val confidence: Float,
    val detectionMethod: DetectionMethod
)

enum class Platform {
    YOUTUBE, INSTAGRAM, TIKTOK, UNKNOWN
}

enum class DetectionMethod {
    UI_ELEMENT, URL_PATTERN, ACTIVITY_NAME, HEURISTIC
}
```

**責務:**
- 適切なPlatformDetectorの選択
- 検出結果の集約
- 設定に基づくプラットフォームの有効/無効化

### 4. PlatformDetector (Interface)

各プラットフォーム固有の検出ロジック。

```kotlin
interface PlatformDetector {
    val platform: Platform
    fun canHandle(packageName: String): Boolean
    fun detectShortVideo(context: AppContext): DetectionResult
}
```

**実装クラス:**

#### YouTubeDetector
```kotlin
class YouTubeDetector : PlatformDetector {
    override val platform = Platform.YOUTUBE
    
    // 検出方法:
    // 1. resource-id: "shorts_player_fragment"
    // 2. URL pattern: "/shorts/"
    // 3. Tab text: "Shorts"
    // 4. Activity: "com.google.android.youtube.app.shorts.ShortActivity"
}
```

#### InstagramDetector
```kotlin
class InstagramDetector : PlatformDetector {
    override val platform = Platform.INSTAGRAM
    
    // 検出方法:
    // 1. resource-id: "clips_viewer_view_pager"
    // 2. Tab icon/text: "Reels"
    // 3. Activity pattern: "reels"
}
```

#### TikTokDetector
```kotlin
class TikTokDetector : PlatformDetector {
    override val platform = Platform.TIKTOK
    
    // 検出方法:
    // 1. Package name: "com.zhiliaoapp.musically"
    // 2. TikTokは全体がショート動画なので、アプリ起動を検出
}
```

### 5. BlockActionManager

ブロックアクションを実行。

```kotlin
interface BlockActionManager {
    fun executeBlockAction(result: DetectionResult, context: AppContext)
    fun setActionType(type: BlockActionType)
}

enum class BlockActionType {
    OVERLAY,           // オーバーレイ表示
    NAVIGATE_BACK,     // 前の画面に戻る
    NOTIFICATION,      // 通知のみ
    COMBINED           // オーバーレイ + 通知
}
```

**実装クラス:**

#### OverlayBlockAction
```kotlin
class OverlayBlockAction : BlockAction {
    // WindowManagerを使用してオーバーレイを表示
    // TYPE_ACCESSIBILITY_OVERLAY を使用
}
```

#### NavigateBackAction
```kotlin
class NavigateBackAction : BlockAction {
    // performGlobalAction(GLOBAL_ACTION_BACK) を使用
}
```

#### NotificationAction
```kotlin
class NotificationAction : BlockAction {
    // NotificationManagerで通知を表示
}
```

### 6. ConfigurationManager

ユーザー設定を管理。

```kotlin
interface ConfigurationManager {
    fun isEnabled(): Boolean
    fun isPlatformEnabled(platform: Platform): Boolean
    fun getBlockActionType(): BlockActionType
    fun getTemporaryDisableEndTime(): Long?
    fun setTemporaryDisable(durationMinutes: Int)
}
```

### 7. SettingsRepository

設定データの永続化。

```kotlin
interface SettingsRepository {
    suspend fun saveSettings(settings: AppSettings)
    suspend fun getSettings(): AppSettings
    fun observeSettings(): Flow<AppSettings>
}

data class AppSettings(
    val isEnabled: Boolean,
    val enabledPlatforms: Set<Platform>,
    val blockActionType: BlockActionType,
    val temporaryDisableEndTime: Long?
)
```


## Data Models

### Room Database Schema

```kotlin
@Database(
    entities = [
        SettingsEntity::class,
        BlockLogEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun blockLogDao(): BlockLogDao
}
```

### SettingsEntity

```kotlin
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isEnabled: Boolean,
    val enabledPlatforms: String, // JSON array of platform names
    val blockActionType: String,
    val temporaryDisableEndTime: Long?
)
```

### BlockLogEntity

```kotlin
@Entity(tableName = "block_logs")
data class BlockLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val platform: String,
    val detectionMethod: String,
    val actionTaken: String,
    val packageName: String
)
```

### SharedPreferences (軽量設定用)

```kotlin
object PreferenceKeys {
    const val IS_SERVICE_ENABLED = "is_service_enabled"
    const val FIRST_LAUNCH = "first_launch"
    const val SHOW_TUTORIAL = "show_tutorial"
}
```

## Detection Logic Details

### YouTube Shorts 検出戦略

**優先度1: UI要素による検出**
```kotlin
fun detectYouTubeShortsFromUI(nodes: List<AccessibilityNodeInfo>): Boolean {
    // resource-id を検索
    val shortsIndicators = listOf(
        "shorts_player_fragment",
        "reel_player_page_container",
        "shorts_container"
    )
    
    return nodes.any { node ->
        shortsIndicators.any { indicator ->
            node.viewIdResourceName?.contains(indicator) == true
        }
    }
}
```

**優先度2: URL パターン検出**
```kotlin
fun detectYouTubeShortsFromURL(nodes: List<AccessibilityNodeInfo>): Boolean {
    return nodes.any { node ->
        node.text?.toString()?.contains("/shorts/") == true
    }
}
```

**優先度3: Activity名検出**
```kotlin
fun detectYouTubeShortsFromActivity(activityName: String?): Boolean {
    return activityName?.contains("short", ignoreCase = true) == true
}
```

### Instagram Reels 検出戦略

**優先度1: UI要素による検出**
```kotlin
fun detectInstagramReelsFromUI(nodes: List<AccessibilityNodeInfo>): Boolean {
    val reelsIndicators = listOf(
        "clips_viewer_view_pager",
        "clips_viewer",
        "reels_viewer"
    )
    
    return nodes.any { node ->
        reelsIndicators.any { indicator ->
            node.viewIdResourceName?.contains(indicator) == true
        }
    }
}
```

**優先度2: タブ検出**
```kotlin
fun detectInstagramReelsTab(nodes: List<AccessibilityNodeInfo>): Boolean {
    return nodes.any { node ->
        node.className == "android.widget.ImageView" &&
        node.contentDescription?.contains("Reels", ignoreCase = true) == true
    }
}
```

### TikTok 検出戦略

TikTokは全体がショート動画プラットフォームなので、アプリの起動自体を検出：

```kotlin
fun detectTikTok(packageName: String): Boolean {
    val tiktokPackages = listOf(
        "com.zhiliaoapp.musically",  // TikTok
        "com.ss.android.ugc.trill"   // TikTok Lite
    )
    return packageName in tiktokPackages
}
```

## Block Action Implementation

### Overlay Display

```kotlin
class OverlayBlockAction(
    private val context: Context,
    private val windowManager: WindowManager
) : BlockAction {
    
    private var overlayView: View? = null
    
    override fun execute(result: DetectionResult) {
        if (overlayView != null) return // Already showing
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        
        overlayView = createOverlayView(result.platform)
        windowManager.addView(overlayView, params)
        
        // Auto-dismiss after delay or user action
        scheduleAutoDismiss()
    }
    
    private fun createOverlayView(platform: Platform): View {
        // Inflate custom layout with message and "Go Back" button
    }
}
```

### Navigate Back Action

```kotlin
class NavigateBackAction(
    private val service: AccessibilityService
) : BlockAction {
    
    override fun execute(result: DetectionResult) {
        // Perform back navigation
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        
        // Show brief notification
        showToast("Shorts blocked - navigated back")
    }
}
```


## Error Handling

### Error Categories

1. **Permission Errors**
   - AccessibilityService not enabled
   - Overlay permission not granted
   - Notification permission not granted (Android 13+)

2. **Detection Errors**
   - UI structure changed (app update)
   - Unexpected node tree structure
   - Null pointer exceptions

3. **Action Errors**
   - Overlay cannot be displayed
   - Back navigation fails
   - Notification fails

### Error Handling Strategy

```kotlin
sealed class BlockerError {
    data class PermissionError(val permission: String) : BlockerError()
    data class DetectionError(val platform: Platform, val cause: Throwable) : BlockerError()
    data class ActionError(val action: BlockActionType, val cause: Throwable) : BlockerError()
}

interface ErrorHandler {
    fun handleError(error: BlockerError)
    fun logError(error: BlockerError)
    fun notifyUser(error: BlockerError)
}

class DefaultErrorHandler : ErrorHandler {
    override fun handleError(error: BlockerError) {
        when (error) {
            is BlockerError.PermissionError -> {
                // Show permission request dialog
                notifyUser(error)
            }
            is BlockerError.DetectionError -> {
                // Log error, continue with other platforms
                logError(error)
                // Don't notify user for detection errors
            }
            is BlockerError.ActionError -> {
                // Log error, try fallback action
                logError(error)
                tryFallbackAction()
            }
        }
    }
}
```

### Graceful Degradation

```kotlin
class RobustDetectionManager(
    private val detectors: List<PlatformDetector>,
    private val errorHandler: ErrorHandler
) {
    fun detectWithFallback(context: AppContext): DetectionResult? {
        return detectors
            .filter { it.canHandle(context.packageName) }
            .firstNotNullOfOrNull { detector ->
                try {
                    detector.detectShortVideo(context)
                } catch (e: Exception) {
                    errorHandler.handleError(
                        BlockerError.DetectionError(detector.platform, e)
                    )
                    null
                }
            }
    }
}
```

### Safe Mode

連続エラーが発生した場合、セーフモードに切り替え：

```kotlin
class SafeModeManager {
    private var consecutiveErrors = 0
    private val errorThreshold = 5
    
    fun recordError() {
        consecutiveErrors++
        if (consecutiveErrors >= errorThreshold) {
            enterSafeMode()
        }
    }
    
    private fun enterSafeMode() {
        // Disable all detection temporarily
        // Notify user
        // Reset after cooldown period
    }
}
```

## Testing Strategy

### Unit Tests

**1. Detector Tests**
```kotlin
@Test
fun `YouTubeDetector detects shorts from UI elements`() {
    val mockNodes = createMockNodesWithShortsIndicator()
    val detector = YouTubeDetector()
    val result = detector.detectShortVideo(mockContext)
    
    assertTrue(result.isShortVideo)
    assertEquals(Platform.YOUTUBE, result.platform)
}
```

**2. Action Tests**
```kotlin
@Test
fun `OverlayBlockAction displays overlay correctly`() {
    val action = OverlayBlockAction(mockContext, mockWindowManager)
    action.execute(mockDetectionResult)
    
    verify(mockWindowManager).addView(any(), any())
}
```

**3. Configuration Tests**
```kotlin
@Test
fun `ConfigurationManager respects platform enable state`() {
    configManager.setPlatformEnabled(Platform.YOUTUBE, false)
    
    assertFalse(configManager.isPlatformEnabled(Platform.YOUTUBE))
}
```

### Integration Tests

**1. End-to-End Detection Flow**
```kotlin
@Test
fun `Full detection flow from event to action`() {
    // Simulate accessibility event
    val event = createYouTubeShortsEvent()
    
    // Process through service
    service.onAccessibilityEvent(event)
    
    // Verify action was executed
    verify(mockActionManager).executeBlockAction(any(), any())
}
```

**2. Multi-Platform Tests**
```kotlin
@Test
fun `Correctly handles multiple platforms`() {
    // Test YouTube
    val youtubeEvent = createYouTubeShortsEvent()
    service.onAccessibilityEvent(youtubeEvent)
    
    // Test Instagram
    val instagramEvent = createInstagramReelsEvent()
    service.onAccessibilityEvent(instagramEvent)
    
    // Verify both were detected correctly
}
```

### Manual Testing Checklist

- [ ] YouTube Shorts tab tap → blocked
- [ ] YouTube Shorts from home feed → blocked
- [ ] YouTube regular video → not blocked
- [ ] Instagram Reels tab tap → blocked
- [ ] Instagram feed → not blocked
- [ ] TikTok app launch → blocked
- [ ] Settings changes apply immediately
- [ ] Temporary disable works correctly
- [ ] Overlay displays and dismisses properly
- [ ] Back navigation works
- [ ] Notifications appear correctly
- [ ] Battery usage is acceptable
- [ ] No crashes during 24-hour test
- [ ] Works after target app updates

### Performance Tests

```kotlin
@Test
fun `Event processing completes within 50ms`() {
    val startTime = System.currentTimeMillis()
    
    eventProcessor.processEvent(mockEvent)
    
    val duration = System.currentTimeMillis() - startTime
    assertTrue(duration < 50)
}

@Test
fun `Memory usage stays under 50MB`() {
    // Run service for extended period
    // Monitor memory usage
    val memoryUsage = getMemoryUsage()
    assertTrue(memoryUsage < 50 * 1024 * 1024) // 50MB in bytes
}
```

## Performance Optimization

### Event Filtering

```kotlin
class OptimizedEventProcessor : AccessibilityEventProcessor {
    
    // Only process relevant event types
    private val relevantEventTypes = setOf(
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    )
    
    // Debounce rapid events
    private val eventDebouncer = Debouncer(delayMs = 100)
    
    override fun processEvent(event: AccessibilityEvent): AppContext? {
        if (event.eventType !in relevantEventTypes) return null
        if (!isTargetPackage(event.packageName)) return null
        
        return eventDebouncer.debounce {
            extractAppContext(event)
        }
    }
}
```

### Caching Strategy

```kotlin
class CachedDetectionManager(
    private val detectorManager: PlatformDetectorManager
) {
    private val cache = LruCache<String, DetectionResult>(maxSize = 20)
    
    fun detectWithCache(context: AppContext): DetectionResult? {
        val cacheKey = "${context.packageName}:${context.activityName}"
        
        return cache.get(cacheKey) ?: run {
            val result = detectorManager.detectShortVideo(context)
            result?.let { cache.put(cacheKey, it) }
            result
        }
    }
}
```

### Background Thread Processing

```kotlin
class AsyncEventProcessor(
    private val coroutineScope: CoroutineScope
) {
    fun processEventAsync(event: AccessibilityEvent) {
        coroutineScope.launch(Dispatchers.Default) {
            val context = extractContext(event)
            val result = detectShortVideo(context)
            
            if (result?.isShortVideo == true) {
                withContext(Dispatchers.Main) {
                    executeBlockAction(result)
                }
            }
        }
    }
}
```

## Security and Privacy

### Data Collection Policy

**収集するデータ:**
- ブロックイベントのログ（タイムスタンプ、プラットフォーム、アクション）
- ユーザー設定

**収集しないデータ:**
- 画面のテキストコンテンツ
- ユーザーの個人情報
- 他のアプリの使用状況（対象アプリ以外）
- 位置情報

### Data Storage

```kotlin
// すべてのデータはローカルに保存
class SecureStorageManager(
    private val database: AppDatabase,
    private val encryptedPrefs: SharedPreferences
) {
    // 機密設定は暗号化
    fun saveSecureSetting(key: String, value: String) {
        encryptedPrefs.edit()
            .putString(key, value)
            .apply()
    }
}
```

### Accessibility Service Declaration

```xml
<!-- accessibility_service_config.xml -->
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:packageNames="com.google.android.youtube,com.instagram.android,com.zhiliaoapp.musically"
    android:settingsActivity="com.example.shortblocker.SettingsActivity" />
```

## UI/UX Design

### Main Screen

```
┌─────────────────────────────────┐
│  Short Video Blocker            │
├─────────────────────────────────┤
│                                 │
│  [Toggle] Service Enabled       │
│                                 │
│  Blocked Platforms:             │
│  ☑ YouTube Shorts               │
│  ☑ Instagram Reels              │
│  ☑ TikTok                       │
│                                 │
│  Block Method:                  │
│  ○ Show Overlay                 │
│  ● Navigate Back                │
│  ○ Notification Only            │
│                                 │
│  [Button] Disable for 30 min    │
│                                 │
│  Statistics:                    │
│  Today: 12 blocks               │
│  This week: 87 blocks           │
│                                 │
└─────────────────────────────────┘
```

### Overlay Design

```
┌─────────────────────────────────┐
│                                 │
│         🚫                      │
│                                 │
│   Short Video Blocked           │
│                                 │
│   This content is restricted    │
│   by your settings              │
│                                 │
│   [Go Back]  [Disable 30min]    │
│                                 │
└─────────────────────────────────┘
```

## Deployment Considerations

### Minimum SDK Version
- minSdk: 24 (Android 7.0)
- targetSdk: 34 (Android 14)

### Required Permissions

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### App Size Optimization
- ProGuard/R8 for code shrinking
- Vector drawables instead of PNGs
- No unnecessary dependencies
- Target size: < 5MB

### Battery Optimization
- Request battery optimization exemption
- Use JobScheduler for non-critical tasks
- Minimize wake locks

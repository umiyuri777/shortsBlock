# Short Video Blocker

An Android application that selectively blocks short-form video content (YouTube Shorts, Instagram Reels, TikTok) while allowing access to regular content.

## Project Structure

```
app/src/main/java/com/example/shortblocker/
├── service/     # AccessibilityService and related components
├── detector/    # Platform-specific detection logic
├── action/      # Block action implementations
├── config/      # Configuration management
├── data/        # Data models, Room database, repositories
└── ui/          # Activities, fragments, ViewModels
```

## Requirements

- Android Studio Hedgehog or later
- Kotlin 1.9.20
- minSdk 24 (Android 7.0)
- targetSdk 34 (Android 14)

## Dependencies

- **Room**: Local database for settings and logs
- **Coroutines**: Asynchronous programming
- **Hilt**: Dependency injection
- **Material Components**: UI components

## Build

```bash
./gradlew build
```

## 実機での実行方法

このアプリをAndroid実機で実行するには、以下の手順に従ってください。

### 1. 実機の準備

#### Android端末の設定

1. **開発者オプションを有効化**
   - 設定 > 端末情報（または「デバイス情報」）を開く
   - 「ビルド番号」を7回タップ
   - 「開発者になりました」というメッセージが表示されます

2. **USBデバッグを有効化**
   - 設定 > 開発者向けオプションを開く
   - 「USBデバッグ」を有効にする
   - 確認ダイアログで「OK」を選択

3. **USB接続**
   - USBケーブルで実機をPCに接続
   - 実機に「USBデバッグを許可しますか？」というダイアログが表示されたら「許可」を選択
   - 「常にこのコンピュータから許可する」にチェックを入れると次回から確認不要です

### 2. Android Studioでの実行

1. **プロジェクトを開く**
   ```bash
   # Android Studioでプロジェクトを開く
   # または
   open -a "Android Studio" .
   ```

2. **実機を認識させる**
   - Android Studioの下部にある「Device Manager」または「Logcat」タブを確認
   - 接続されたデバイスが表示されることを確認
   - 表示されない場合は、USBケーブルを抜き差しするか、`adb devices`コマンドで確認

3. **ビルド設定の確認**
   - 上部のツールバーで「debug」ビルドバリアントが選択されていることを確認
   - 実行対象デバイスとして実機が選択されていることを確認

4. **アプリを実行**
   - 緑の実行ボタン（▶）をクリック
   - または `Shift + F10`（Mac: `Ctrl + R`）
   - またはコマンドラインから：
     ```bash
     ./gradlew installDebug
     ```

### 3. アプリの権限設定

このアプリはAccessibilityServiceを使用するため、インストール後に手動で権限を有効化する必要があります。

1. **アクセシビリティサービスを有効化**
   - 実機の「設定」アプリを開く
   - 「アクセシビリティ」（または「ユーザー補助」）を開く
   - 「インストール済みのサービス」から「Short Video Blocker」（または「ShortBlocker」）を探す
   - サービスを有効にする
   - 確認ダイアログで「許可」を選択

2. **オーバーレイ権限の許可（オプション）**
   - オーバーレイ機能を使用する場合：
     - 設定 > アプリ > Short Video Blocker > 他のアプリの上に表示
     - または設定 > アプリ > 特別なアクセス > 他のアプリの上に表示
   - 「Short Video Blocker」を選択して有効化

3. **通知権限の許可（オプション）**
   - Android 13以降の場合、通知権限の許可が必要です
   - アプリ起動時に表示されるダイアログで「許可」を選択

### 4. 動作確認

1. **アプリを起動**
   - ホーム画面から「Short Video Blocker」を起動
   - メイン画面が表示されることを確認

2. **設定を確認**
   - 設定画面でブロック対象（YouTube Shorts、Instagram Reels、TikTok）を有効化
   - ブロックアクション（オーバーレイ、戻る、通知）を選択

3. **実際にテスト**
   - YouTubeアプリを開いてShortsを表示してみる
   - Instagramアプリを開いてReelsを表示してみる
   - TikTokアプリを開いてみる
   - それぞれがブロックされることを確認

### トラブルシューティング

#### 実機が認識されない場合

```bash
# ADBでデバイスを確認
adb devices

# デバイスが表示されない場合
adb kill-server
adb start-server
adb devices
```

#### アプリがインストールされない場合

- 実機の「不明なソースからのアプリのインストール」を許可する
- 実機のストレージ容量を確認する
- Android Studioのログ（Logcat）でエラーメッセージを確認する

#### AccessibilityServiceが動作しない場合

- 設定 > アクセシビリティでサービスが有効になっているか確認
- アプリを再起動してみる
- 実機を再起動してみる
- Android StudioのLogcatでエラーログを確認する

#### ビルドエラーが発生する場合

```bash
# クリーンビルドを実行
./gradlew clean
./gradlew build

# Gradleのキャッシュをクリア
./gradlew cleanBuildCache
```

### コマンドラインからの実行

Android Studioを使わずにコマンドラインから実行する場合：

```bash
# デバッグAPKをビルドしてインストール
./gradlew installDebug

# リリースAPKをビルド（署名が必要）
./gradlew assembleRelease

# 特定のデバイスにインストール
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Features

- Detects and blocks YouTube Shorts
- Detects and blocks Instagram Reels
- Blocks TikTok app entirely
- Customizable block actions (overlay, navigate back, notification)
- Temporary disable functionality
- Usage statistics

## Architecture

The app uses AccessibilityService to monitor screen content and detect short-form video sections. When detected, it executes the configured block action.

See `.kiro/specs/short-video-blocker/design.md` for detailed architecture documentation.

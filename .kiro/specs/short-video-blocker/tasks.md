# Implementation Plan

- [x] 1. プロジェクト構造とコア依存関係のセットアップ

  - Android Studio プロジェクトの作成（Kotlin、minSdk 24、targetSdk 34）
  - build.gradle に必要な依存関係を追加（Room、Coroutines、Hilt/Koin）
  - パッケージ構造の作成（service、detector、action、config、data、ui）
  - _Requirements: 4.1, 4.2_

- [x] 2. データモデルとローカルストレージの実装

  - [x] 2.1 Room データベースのセットアップ

    - AppDatabase クラスの作成
    - SettingsEntity と BlockLogEntity の定義
    - DAO インターフェースの実装（SettingsDao、BlockLogDao）
    - _Requirements: 5.1, 5.2, 8.2_

  - [x] 2.2 データモデルクラスの作成

    - AppSettings、AppContext、DetectionResult、Platform、BlockActionType などの data class を定義
    - Enum 定義（Platform、DetectionMethod、BlockActionType）
    - _Requirements: 5.1, 5.2_

  - [x] 2.3 SettingsRepository の実装
    - SettingsRepository インターフェースと実装クラスの作成
    - SharedPreferences と Room の統合
    - Flow ベースの設定監視機能
    - _Requirements: 5.1, 5.2, 5.5_

- [x] 3. AccessibilityService の基本実装

  - [x] 3.1 BlockerAccessibilityService クラスの作成

    - AccessibilityService を継承
    - onAccessibilityEvent、onInterrupt、onServiceConnected のオーバーライド
    - サービスライフサイクル管理
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 3.2 accessibility_service_config.xml の作成

    - イベントタイプの設定（typeWindowStateChanged、typeWindowContentChanged）
    - 対象パッケージの指定（YouTube、Instagram、TikTok）
    - サービス説明とフラグの設定
    - _Requirements: 4.1, 4.2_

  - [x] 3.3 AndroidManifest.xml の設定
    - AccessibilityService の宣言
    - 必要な権限の追加（SYSTEM_ALERT_WINDOW、POST_NOTIFICATIONS）
    - _Requirements: 4.1, 4.2_

- [x] 4. イベント処理とコンテキスト抽出の実装

  - [x] 4.1 AccessibilityEventProcessor の実装

    - AccessibilityEvent から AppContext を抽出
    - AccessibilityNodeInfo ツリーの構築
    - イベントフィルタリングロジック（対象パッケージのみ処理）
    - _Requirements: 4.3, 6.1, 6.3_

  - [x] 4.2 イベントデバウンサーの実装
    - 連続イベントの抑制（100ms 間隔）
    - パフォーマンス最適化
    - _Requirements: 6.1, 6.2_

- [x] 5. プラットフォーム検出ロジックの実装

  - [x] 5.1 PlatformDetector インターフェースの定義

    - canHandle、detectShortVideo メソッドの定義
    - 共通の検出ユーティリティメソッド
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 5.2 YouTubeDetector の実装

    - UI 要素検出（resource-id: "shorts_player_fragment"など）
    - URL パターン検出（"/shorts/"）
    - Activity 名検出
    - 優先度ベースの検出ロジック
    - _Requirements: 1.1, 1.4, 3.1_

  - [x] 5.3 InstagramDetector の実装

    - UI 要素検出（resource-id: "clips_viewer_view_pager"など）
    - Reels タブ検出
    - Activity 名検出
    - _Requirements: 1.2, 1.5, 3.2_

  - [x] 5.4 TikTokDetector の実装

    - パッケージ名による検出
    - アプリ起動の検出
    - _Requirements: 3.3_

  - [x] 5.5 PlatformDetectorManager の実装
    - 複数の Detector の管理
    - 適切な Detector の選択とディスパッチ
    - 設定に基づくプラットフォームの有効/無効化
    - エラーハンドリングとフォールバック
    - _Requirements: 1.1, 1.2, 1.3, 3.4, 7.2_

- [x] 6. ブロックアクション機能の実装

  - [x] 6.1 BlockAction インターフェースの定義

    - execute メソッドの定義
    - 共通のアクションユーティリティ
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 6.2 OverlayBlockAction の実装

    - WindowManager を使用したオーバーレイ表示
    - TYPE_ACCESSIBILITY_OVERLAY の使用
    - オーバーレイ UI の作成（メッセージ、戻るボタン）
    - 自動消去タイマー
    - _Requirements: 2.2, 2.3, 5.3_

  - [x] 6.3 NavigateBackAction の実装

    - performGlobalAction(GLOBAL_ACTION_BACK)の使用
    - 簡易通知の表示
    - _Requirements: 2.1, 2.3, 2.4_

  - [x] 6.4 NotificationAction の実装

    - NotificationManager を使用した通知表示
    - 通知チャンネルの作成（Android 8.0+）
    - 通知内容のカスタマイズ
    - _Requirements: 2.2, 5.3_

  - [x] 6.5 BlockActionManager の実装
    - アクションタイプに基づく適切なアクションの実行
    - 複合アクション（オーバーレイ + 通知）のサポート
    - エラーハンドリングとフォールバック
    - _Requirements: 2.1, 2.2, 2.3, 7.3_

- [x] 7. 設定管理と ConfigurationManager の実装

  - [x] 7.1 ConfigurationManager の実装

    - SettingsRepository との統合
    - プラットフォーム有効/無効の管理
    - ブロックアクションタイプの管理
    - 一時無効化機能（タイマー管理）
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 7.2 一時無効化タイマーの実装
    - AlarmManager または WorkManager を使用
    - 指定時間後の自動再有効化
    - 通知による状態表示
    - _Requirements: 5.5_

- [x] 8. エラーハンドリングとロギングの実装

  - [x] 8.1 ErrorHandler の実装

    - BlockerError の sealed class 定義
    - エラーカテゴリ別の処理（Permission、Detection、Action）
    - ユーザー通知ロジック
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 8.2 SafeModeManager の実装

    - 連続エラーの検出
    - セーフモードへの自動切り替え
    - クールダウン期間後の復帰
    - _Requirements: 7.4_

  - [x] 8.3 ログ記録機能の実装
    - BlockLogEntity へのログ保存
    - デバッグログとリリースログの分離
    - ログのローテーション（古いログの削除）
    - _Requirements: 7.1, 8.2_

- [x] 9. UI の実装

  - [x] 9.1 MainActivity とメイン画面の作成

    - サービス有効/無効トグル
    - プラットフォーム選択チェックボックス
    - ブロック方法の選択（ラジオボタン）
    - 一時無効化ボタン
    - 統計表示（今日/今週のブロック数）
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 9.2 SettingsActivity の作成

    - 詳細設定画面
    - 権限管理画面へのリンク
    - アプリ情報とバージョン表示
    - _Requirements: 5.1, 5.2_

  - [x] 9.3 オーバーレイ UI レイアウトの作成

    - ブロックメッセージ表示
    - 戻るボタンと一時無効化ボタン
    - プラットフォーム別のアイコン表示
    - _Requirements: 2.2, 2.3_

  - [x] 9.4 権限リクエストフローの実装
    - AccessibilityService 有効化ガイド
    - オーバーレイ権限リクエスト
    - 通知権限リクエスト（Android 13+）
    - 権限状態のチェックと案内
    - _Requirements: 4.1, 4.5_

- [ ] 10. ViewModel と UI ロジックの実装

  - [ ] 10.1 MainViewModel の実装

    - 設定の読み込みと監視（Flow）
    - 設定の更新（プラットフォーム、アクションタイプ）
    - 統計データの取得
    - 一時無効化の実行
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 10.2 StatisticsManager の実装
    - BlockLog からの統計計算
    - 日別、週別、月別の集計
    - プラットフォーム別の統計
    - _Requirements: 5.1_

- [ ] 11. 依存性注入のセットアップ

  - [ ] 11.1 DI コンテナの設定（Hilt または Koin）

    - モジュール定義（Database、Repository、Manager、Detector、Action）
    - シングルトンとスコープの設定
    - _Requirements: すべて_

  - [ ] 11.2 各コンポーネントへの DI 統合
    - コンストラクタインジェクション
    - ViewModel へのインジェクション
    - AccessibilityService へのインジェクション（手動取得）
    - _Requirements: すべて_

- [ ] 12. パフォーマンス最適化の実装

  - [ ] 12.1 キャッシング機能の追加

    - LruCache を使用した検出結果のキャッシュ
    - キャッシュキーの設計（packageName + activityName）
    - _Requirements: 6.1, 6.2_

  - [ ] 12.2 非同期処理の最適化
    - Coroutines を使用した非同期イベント処理
    - Dispatchers.Default での検出処理
    - Dispatchers.Main でのアクション実行
    - _Requirements: 6.1, 6.2_

- [ ] 13. 統合とエンドツーエンドの接続

  - [ ] 13.1 全コンポーネントの統合

    - BlockerAccessibilityService に全コンポーネントを統合
    - イベント → 処理 → 検出 → アクションのフロー確立
    - エラーハンドリングの統合
    - _Requirements: すべて_

  - [ ] 13.2 アプリ起動時の初期化処理
    - データベースの初期化
    - デフォルト設定の作成
    - 権限チェック
    - _Requirements: 4.1, 4.5, 5.1_

- [ ] 14. リソースとローカライゼーション

  - [ ] 14.1 文字列リソースの作成

    - strings.xml に全テキストを定義
    - AccessibilityService の説明文
    - UI 要素のテキスト
    - エラーメッセージ
    - _Requirements: 4.2, 5.1_

  - [ ] 14.2 アイコンとドローアブルの追加
    - アプリアイコン
    - プラットフォームアイコン（YouTube、Instagram、TikTok）
    - ブロックアイコン
    - ベクタードローアブルの使用
    - _Requirements: 5.1_

- [ ] 15. ProGuard とリリース設定

  - [ ] 15.1 ProGuard/R8 ルールの設定

    - Room エンティティの保持
    - Kotlin リフレクション対応
    - 難読化ルール
    - _Requirements: 6.4_

  - [ ] 15.2 リリースビルド設定
    - 署名設定
    - バージョン管理
    - minifyEnabled の有効化
    - _Requirements: 6.4_

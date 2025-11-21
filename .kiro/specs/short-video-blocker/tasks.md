# Implementation Plan

- [x] 1. プロジェクト構造とコア依存関係のセットアップ
  - Android Studioプロジェクトの作成（Kotlin、minSdk 24、targetSdk 34）
  - build.gradleに必要な依存関係を追加（Room、Coroutines、Hilt/Koin）
  - パッケージ構造の作成（service、detector、action、config、data、ui）
  - _Requirements: 4.1, 4.2_

- [x] 2. データモデルとローカルストレージの実装
  - [x] 2.1 Roomデータベースのセットアップ
    - AppDatabaseクラスの作成
    - SettingsEntityとBlockLogEntityの定義
    - DAOインターフェースの実装（SettingsDao、BlockLogDao）
    - _Requirements: 5.1, 5.2, 8.2_
  
  - [x] 2.2 データモデルクラスの作成
    - AppSettings、AppContext、DetectionResult、Platform、BlockActionTypeなどのdata classを定義
    - Enum定義（Platform、DetectionMethod、BlockActionType）
    - _Requirements: 5.1, 5.2_
  
  - [x] 2.3 SettingsRepositoryの実装
    - SettingsRepositoryインターフェースと実装クラスの作成
    - SharedPreferencesとRoomの統合
    - Flowベースの設定監視機能
    - _Requirements: 5.1, 5.2, 5.5_

- [ ] 3. AccessibilityServiceの基本実装
  - [ ] 3.1 BlockerAccessibilityServiceクラスの作成
    - AccessibilityServiceを継承
    - onAccessibilityEvent、onInterrupt、onServiceConnectedのオーバーライド
    - サービスライフサイクル管理
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [ ] 3.2 accessibility_service_config.xmlの作成
    - イベントタイプの設定（typeWindowStateChanged、typeWindowContentChanged）
    - 対象パッケージの指定（YouTube、Instagram、TikTok）
    - サービス説明とフラグの設定
    - _Requirements: 4.1, 4.2_
  
  - [ ] 3.3 AndroidManifest.xmlの設定
    - AccessibilityServiceの宣言
    - 必要な権限の追加（SYSTEM_ALERT_WINDOW、POST_NOTIFICATIONS）
    - _Requirements: 4.1, 4.2_

- [ ] 4. イベント処理とコンテキスト抽出の実装
  - [ ] 4.1 AccessibilityEventProcessorの実装
    - AccessibilityEventからAppContextを抽出
    - AccessibilityNodeInfoツリーの構築
    - イベントフィルタリングロジック（対象パッケージのみ処理）
    - _Requirements: 4.3, 6.1, 6.3_
  
  - [ ] 4.2 イベントデバウンサーの実装
    - 連続イベントの抑制（100ms間隔）
    - パフォーマンス最適化
    - _Requirements: 6.1, 6.2_

- [ ] 5. プラットフォーム検出ロジックの実装
  - [ ] 5.1 PlatformDetectorインターフェースの定義
    - canHandle、detectShortVideoメソッドの定義
    - 共通の検出ユーティリティメソッド
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ] 5.2 YouTubeDetectorの実装
    - UI要素検出（resource-id: "shorts_player_fragment"など）
    - URLパターン検出（"/shorts/"）
    - Activity名検出
    - 優先度ベースの検出ロジック
    - _Requirements: 1.1, 1.4, 3.1_
  
  - [ ] 5.3 InstagramDetectorの実装
    - UI要素検出（resource-id: "clips_viewer_view_pager"など）
    - Reelsタブ検出
    - Activity名検出
    - _Requirements: 1.2, 1.5, 3.2_
  
  - [ ] 5.4 TikTokDetectorの実装
    - パッケージ名による検出
    - アプリ起動の検出
    - _Requirements: 3.3_
  
  - [ ] 5.5 PlatformDetectorManagerの実装
    - 複数のDetectorの管理
    - 適切なDetectorの選択とディスパッチ
    - 設定に基づくプラットフォームの有効/無効化
    - エラーハンドリングとフォールバック
    - _Requirements: 1.1, 1.2, 1.3, 3.4, 7.2_

- [ ] 6. ブロックアクション機能の実装
  - [ ] 6.1 BlockActionインターフェースの定義
    - executeメソッドの定義
    - 共通のアクションユーティリティ
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [ ] 6.2 OverlayBlockActionの実装
    - WindowManagerを使用したオーバーレイ表示
    - TYPE_ACCESSIBILITY_OVERLAYの使用
    - オーバーレイUIの作成（メッセージ、戻るボタン）
    - 自動消去タイマー
    - _Requirements: 2.2, 2.3, 5.3_
  
  - [ ] 6.3 NavigateBackActionの実装
    - performGlobalAction(GLOBAL_ACTION_BACK)の使用
    - 簡易通知の表示
    - _Requirements: 2.1, 2.3, 2.4_
  
  - [ ] 6.4 NotificationActionの実装
    - NotificationManagerを使用した通知表示
    - 通知チャンネルの作成（Android 8.0+）
    - 通知内容のカスタマイズ
    - _Requirements: 2.2, 5.3_
  
  - [ ] 6.5 BlockActionManagerの実装
    - アクションタイプに基づく適切なアクションの実行
    - 複合アクション（オーバーレイ + 通知）のサポート
    - エラーハンドリングとフォールバック
    - _Requirements: 2.1, 2.2, 2.3, 7.3_

- [ ] 7. 設定管理とConfigurationManagerの実装
  - [ ] 7.1 ConfigurationManagerの実装
    - SettingsRepositoryとの統合
    - プラットフォーム有効/無効の管理
    - ブロックアクションタイプの管理
    - 一時無効化機能（タイマー管理）
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [ ] 7.2 一時無効化タイマーの実装
    - AlarmManagerまたはWorkManagerを使用
    - 指定時間後の自動再有効化
    - 通知による状態表示
    - _Requirements: 5.5_

- [ ] 8. エラーハンドリングとロギングの実装
  - [ ] 8.1 ErrorHandlerの実装
    - BlockerErrorのsealed class定義
    - エラーカテゴリ別の処理（Permission、Detection、Action）
    - ユーザー通知ロジック
    - _Requirements: 7.1, 7.2, 7.3_
  
  - [ ] 8.2 SafeModeManagerの実装
    - 連続エラーの検出
    - セーフモードへの自動切り替え
    - クールダウン期間後の復帰
    - _Requirements: 7.4_
  
  - [ ] 8.3 ログ記録機能の実装
    - BlockLogEntityへのログ保存
    - デバッグログとリリースログの分離
    - ログのローテーション（古いログの削除）
    - _Requirements: 7.1, 8.2_

- [ ] 9. UIの実装
  - [ ] 9.1 MainActivityとメイン画面の作成
    - サービス有効/無効トグル
    - プラットフォーム選択チェックボックス
    - ブロック方法の選択（ラジオボタン）
    - 一時無効化ボタン
    - 統計表示（今日/今週のブロック数）
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [ ] 9.2 SettingsActivityの作成
    - 詳細設定画面
    - 権限管理画面へのリンク
    - アプリ情報とバージョン表示
    - _Requirements: 5.1, 5.2_
  
  - [ ] 9.3 オーバーレイUIレイアウトの作成
    - ブロックメッセージ表示
    - 戻るボタンと一時無効化ボタン
    - プラットフォーム別のアイコン表示
    - _Requirements: 2.2, 2.3_
  
  - [ ] 9.4 権限リクエストフローの実装
    - AccessibilityService有効化ガイド
    - オーバーレイ権限リクエスト
    - 通知権限リクエスト（Android 13+）
    - 権限状態のチェックと案内
    - _Requirements: 4.1, 4.5_

- [ ] 10. ViewModelとUIロジックの実装
  - [ ] 10.1 MainViewModelの実装
    - 設定の読み込みと監視（Flow）
    - 設定の更新（プラットフォーム、アクションタイプ）
    - 統計データの取得
    - 一時無効化の実行
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [ ] 10.2 StatisticsManagerの実装
    - BlockLogからの統計計算
    - 日別、週別、月別の集計
    - プラットフォーム別の統計
    - _Requirements: 5.1_

- [ ] 11. 依存性注入のセットアップ
  - [ ] 11.1 DIコンテナの設定（HiltまたはKoin）
    - モジュール定義（Database、Repository、Manager、Detector、Action）
    - シングルトンとスコープの設定
    - _Requirements: すべて_
  
  - [ ] 11.2 各コンポーネントへのDI統合
    - コンストラクタインジェクション
    - ViewModelへのインジェクション
    - AccessibilityServiceへのインジェクション（手動取得）
    - _Requirements: すべて_

- [ ] 12. パフォーマンス最適化の実装
  - [ ] 12.1 キャッシング機能の追加
    - LruCacheを使用した検出結果のキャッシュ
    - キャッシュキーの設計（packageName + activityName）
    - _Requirements: 6.1, 6.2_
  
  - [ ] 12.2 非同期処理の最適化
    - Coroutinesを使用した非同期イベント処理
    - Dispatchers.Defaultでの検出処理
    - Dispatchers.Mainでのアクション実行
    - _Requirements: 6.1, 6.2_

- [ ] 13. 統合とエンドツーエンドの接続
  - [ ] 13.1 全コンポーネントの統合
    - BlockerAccessibilityServiceに全コンポーネントを統合
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
    - strings.xmlに全テキストを定義
    - AccessibilityServiceの説明文
    - UI要素のテキスト
    - エラーメッセージ
    - _Requirements: 4.2, 5.1_
  
  - [ ] 14.2 アイコンとドローアブルの追加
    - アプリアイコン
    - プラットフォームアイコン（YouTube、Instagram、TikTok）
    - ブロックアイコン
    - ベクタードローアブルの使用
    - _Requirements: 5.1_

- [ ] 15. ProGuardとリリース設定
  - [ ] 15.1 ProGuard/R8ルールの設定
    - Roomエンティティの保持
    - Kotlinリフレクション対応
    - 難読化ルール
    - _Requirements: 6.4_
  
  - [ ] 15.2 リリースビルド設定
    - 署名設定
    - バージョン管理
    - minifyEnabledの有効化
    - _Requirements: 6.4_

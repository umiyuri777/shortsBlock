package com.example.shortblocker.action

import com.example.shortblocker.data.AppContext
import com.example.shortblocker.data.DetectionResult

/**
 * ブロックアクションを実行するためのインターフェース
 */
interface BlockAction {
    /**
     * ブロックアクションを実行する
     * @param result 検出結果
     * @param context アプリケーションコンテキスト
     */
    suspend fun execute(result: DetectionResult, context: AppContext)
    
    /**
     * アクションが現在実行可能かどうかを確認
     * @return 実行可能な場合はtrue
     */
    fun canExecute(): Boolean = true
    
    /**
     * アクションのクリーンアップ処理
     */
    fun cleanup()
}

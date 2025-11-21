package com.example.shortblocker.data

enum class BlockActionType {
    OVERLAY,           // オーバーレイ表示
    NAVIGATE_BACK,     // 前の画面に戻る
    NOTIFICATION,      // 通知のみ
    COMBINED           // オーバーレイ + 通知
}

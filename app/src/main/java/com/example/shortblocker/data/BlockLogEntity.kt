package com.example.shortblocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_logs")
data class BlockLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val platform: String,
    val detectionMethod: String,
    val actionTaken: String,
    val packageName: String
)

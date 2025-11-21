package com.example.shortblocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockLogDao {
    @Insert
    suspend fun insertLog(log: BlockLogEntity)
    
    @Insert
    suspend fun insert(log: BlockLogEntity)
    
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<BlockLogEntity>
    
    @Query("SELECT * FROM block_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getLogsSince(startTime: Long): List<BlockLogEntity>
    
    @Query("SELECT * FROM block_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getLogsByTimeRange(startTime: Long, endTime: Long): List<BlockLogEntity>
    
    @Query("SELECT * FROM block_logs WHERE platform = :platform ORDER BY timestamp DESC")
    suspend fun getLogsByPlatform(platform: String): List<BlockLogEntity>
    
    @Query("SELECT COUNT(*) FROM block_logs WHERE timestamp >= :startTime")
    suspend fun getBlockCountSince(startTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM block_logs WHERE timestamp >= :startTime AND platform = :platform")
    suspend fun getBlockCountByPlatformSince(platform: String, startTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM block_logs")
    suspend fun getLogCount(): Int
    
    @Query("DELETE FROM block_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long): Int
    
    @Query("DELETE FROM block_logs")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC")
    fun observeAllLogs(): Flow<List<BlockLogEntity>>
}

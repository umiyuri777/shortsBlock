package com.example.shortblocker.ui

import com.example.shortblocker.data.BlockLogDao
import com.example.shortblocker.data.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Statistics data for a specific time period
 */
data class BlockStatistics(
    val totalBlocks: Int,
    val youtubeBlocks: Int,
    val instagramBlocks: Int,
    val tiktokBlocks: Int,
    val startTime: Long,
    val endTime: Long
)

/**
 * Manages block statistics calculation and aggregation
 */
@Singleton
class StatisticsManager @Inject constructor(
    private val blockLogDao: BlockLogDao
) {
    
    /**
     * Get statistics for today
     */
    suspend fun getTodayStatistics(): BlockStatistics {
        val startTime = getStartOfDay(System.currentTimeMillis())
        val endTime = System.currentTimeMillis()
        return getStatisticsForPeriod(startTime, endTime)
    }
    
    /**
     * Get statistics for the current week
     */
    suspend fun getWeekStatistics(): BlockStatistics {
        val startTime = getStartOfWeek(System.currentTimeMillis())
        val endTime = System.currentTimeMillis()
        return getStatisticsForPeriod(startTime, endTime)
    }
    
    /**
     * Get statistics for the current month
     */
    suspend fun getMonthStatistics(): BlockStatistics {
        val startTime = getStartOfMonth(System.currentTimeMillis())
        val endTime = System.currentTimeMillis()
        return getStatisticsForPeriod(startTime, endTime)
    }
    
    /**
     * Get statistics for a custom time period
     */
    suspend fun getStatisticsForPeriod(startTime: Long, endTime: Long): BlockStatistics {
        val logs = blockLogDao.getLogsByTimeRange(startTime, endTime)
        
        val youtubeBlocks = logs.count { it.platform == Platform.YOUTUBE.name }
        val instagramBlocks = logs.count { it.platform == Platform.INSTAGRAM.name }
        val tiktokBlocks = logs.count { it.platform == Platform.TIKTOK.name }
        
        return BlockStatistics(
            totalBlocks = logs.size,
            youtubeBlocks = youtubeBlocks,
            instagramBlocks = instagramBlocks,
            tiktokBlocks = tiktokBlocks,
            startTime = startTime,
            endTime = endTime
        )
    }
    
    /**
     * Get statistics by platform for a specific time period
     */
    suspend fun getPlatformStatistics(platform: Platform, startTime: Long): Int {
        return blockLogDao.getBlockCountByPlatformSince(platform.name, startTime)
    }
    
    /**
     * Observe today's block count as a Flow
     */
    fun observeTodayBlockCount(): Flow<Int> {
        return blockLogDao.observeAllLogs().map { logs ->
            val startTime = getStartOfDay(System.currentTimeMillis())
            logs.count { it.timestamp >= startTime }
        }
    }
    
    /**
     * Observe week's block count as a Flow
     */
    fun observeWeekBlockCount(): Flow<Int> {
        return blockLogDao.observeAllLogs().map { logs ->
            val startTime = getStartOfWeek(System.currentTimeMillis())
            logs.count { it.timestamp >= startTime }
        }
    }
    
    /**
     * Get daily statistics for the last N days
     */
    suspend fun getDailyStatistics(days: Int): List<Pair<Long, Int>> {
        val result = mutableListOf<Pair<Long, Int>>()
        val now = System.currentTimeMillis()
        
        for (i in 0 until days) {
            val dayStart = getStartOfDay(now - (i * 24 * 60 * 60 * 1000))
            val dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1
            val count = blockLogDao.getLogsByTimeRange(dayStart, dayEnd).size
            result.add(dayStart to count)
        }
        
        return result.reversed()
    }
    
    /**
     * Get the start of the day timestamp
     */
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get the start of the week timestamp
     */
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
    
    /**
     * Get the start of the month timestamp
     */
    private fun getStartOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

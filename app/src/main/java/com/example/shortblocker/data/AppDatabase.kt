package com.example.shortblocker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SettingsEntity::class,
        BlockLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun blockLogDao(): BlockLogDao
}

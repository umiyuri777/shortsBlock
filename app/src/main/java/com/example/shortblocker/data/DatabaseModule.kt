package com.example.shortblocker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "short_blocker_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideBlockLogDao(database: AppDatabase): BlockLogDao {
        return database.blockLogDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("short_blocker_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDao: SettingsDao,
        sharedPreferences: SharedPreferences
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDao, sharedPreferences)
    }
}

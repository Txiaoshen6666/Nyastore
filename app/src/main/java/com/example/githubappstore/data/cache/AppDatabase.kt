package com.example.githubappstore.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedRepo::class, CachedRelease::class, CachedStarred::class],
    version = 1, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao

    companion object {
        private const val NAME = "gitstore-cache.db"
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .fallbackToDestructiveMigration().build()
    }
}

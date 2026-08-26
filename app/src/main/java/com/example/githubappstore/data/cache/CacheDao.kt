package com.example.githubappstore.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_repos WHERE feed = :feed ORDER BY stars DESC")
    suspend fun reposByFeed(feed: String): List<CachedRepo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRepos(repos: List<CachedRepo>)

    @Query("DELETE FROM cached_repos WHERE feed = :feed")
    suspend fun clearFeed(feed: String): Int

    @Query("SELECT * FROM cached_releases WHERE `key` = :key LIMIT 1")
    suspend fun release(key: String): CachedRelease?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRelease(release: CachedRelease)

    @Query("SELECT json FROM cached_starred ORDER BY cachedAt DESC LIMIT 1")
    suspend fun latestStarred(): String?

    @Query("SELECT * FROM cached_starred ORDER BY cachedAt DESC LIMIT 1")
    suspend fun latestStarredRow(): CachedStarred?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStarred(starred: CachedStarred)

    @Query("DELETE FROM cached_starred")
    suspend fun clearStarred(): Int
}

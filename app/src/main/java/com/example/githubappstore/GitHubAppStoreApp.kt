package com.example.githubappstore

import android.app.Application
import com.example.githubappstore.data.GitHubRepository
import com.example.githubappstore.data.cache.AppDatabase
import com.example.githubappstore.data.cache.CachedGitHubRepository
import com.example.githubappstore.data.remote.GitHubApiService
import com.example.githubappstore.data.settings.AppSettings
import com.example.githubappstore.util.ApkDownloader
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Application entry point. Builds and holds singleton dependencies consumed by
 * ViewModels via the [container] accessor. The GitHub repository is wrapped by
 * [CachedGitHubRepository] (Room, 1-hour TTL) so UI reads are local-first and
 * GitHub API rate-limit pressure is minimised.
 */
class GitHubAppStoreApp : Application() {

    val settings: AppSettings by lazy { AppSettings(this) }

    private val json: Json by lazy {
        Json { ignoreUnknownKeys = true; explicitNulls = false }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val gitHubApi: GitHubApiService by lazy { retrofit.create(GitHubApiService::class.java) }
    val gitHubRepository: GitHubRepository by lazy { GitHubRepository(gitHubApi, settings) }
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val cachedRepository: CachedGitHubRepository by lazy { CachedGitHubRepository(gitHubRepository, database) }
    val apkDownloader: ApkDownloader by lazy { ApkDownloader(this) }

    companion object {
        lateinit var container: GitHubAppStoreApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        container = this
    }
}

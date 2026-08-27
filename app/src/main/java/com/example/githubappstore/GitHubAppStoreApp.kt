package com.example.githubappstore

import android.app.Application
import androidx.room.Room
import com.example.githubappstore.data.cache.AppDatabase
import com.example.githubappstore.data.cache.CachedGitHubRepository
import com.example.githubappstore.data.remote.GitHubApiService
import com.example.githubappstore.data.settings.AppSettings
import com.example.githubappstore.util.ApkDownloader
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class GitHubAppStoreApp : Application() {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    lateinit var database: AppDatabase
        private set

    lateinit var cachedRepository: CachedGitHubRepository
        private set

    lateinit var settings: AppSettings
        private set

    lateinit var apkDownloader: ApkDownloader
        private set

    companion object {
        /** The application instance, exposed as the DI container. Being a
         *  [Application] (i.e. a [android.content.Context]) it can also be passed
         *  directly to components that need a [android.content.Context]. */
        lateinit var container: GitHubAppStoreApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "github_app_store.db"
        ).build()

        settings = AppSettings(this)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(GitHubApiService::class.java)
        cachedRepository = CachedGitHubRepository(api, database) {
            settings.githubToken.first().takeIf { it.isNotBlank() }
        }
        apkDownloader = ApkDownloader(this)
        container = this
    }
}

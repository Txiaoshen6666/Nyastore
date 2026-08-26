package com.example.githubappstore

import android.app.Application
import androidx.room.Room
import com.example.githubappstore.data.cache.AppDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GitHubAppStoreApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var gitHubRepository: com.example.githubappstore.data.GitHubRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "github_app_store.db"
        ).build()

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
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        gitHubRepository = retrofit.create(com.example.githubappstore.data.GitHubRepository::class.java)
    }
}

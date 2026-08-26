package com.example.githubappstore.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gitstore_settings")

/**
 * Persistent app settings (DataStore Preferences).
 *  - Mirror proxy: master [mirrorEnabled] (default on) + [mirrorHost] (default ghfast.top).
 *  - Multi-threaded download: [multiThreadDownload] + [downloadThreadCount] (2..8, default 4).
 *  - Release link via API fallback: [useApiForRelease] (default off).
 *  - Pure-black dark background: [pureBlackDarkMode] (default off).
 *  - Dynamic color (Material You): [dynamicColor] (default on).
 *  - GitHub PAT (local only): [githubToken].
 *  - First-run wizard: [wizardCompleted].
 */
class AppSettings(private val context: Context) {

    val mirrorEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_MIRROR_ENABLED] != false }
    val mirrorHost: Flow<String> = context.dataStore.data.map { it[KEY_MIRROR_HOST].orEmpty().ifBlank { DEFAULT_MIRROR_HOST } }
    val multiThreadDownload: Flow<Boolean> = context.dataStore.data.map { it[KEY_MULTI_THREAD] == true }
    val downloadThreadCount: Flow<Int> = context.dataStore.data.map { (it[KEY_THREAD_COUNT] ?: DEFAULT_THREAD_COUNT).coerceIn(MIN_THREADS, MAX_THREADS) }
    val useApiForRelease: Flow<Boolean> = context.dataStore.data.map { it[KEY_API_RELEASE] == true }
    val pureBlackDarkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_PURE_BLACK_DARK] == true }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] != false }
    val githubToken: Flow<String> = context.dataStore.data.map { it[KEY_GITHUB_TOKEN].orEmpty() }
    val wizardCompleted: Flow<Boolean> = context.dataStore.data.map { it[KEY_WIZARD_COMPLETED] == true }

    suspend fun setMirrorEnabled(v: Boolean) = context.dataStore.edit { it[KEY_MIRROR_ENABLED] = v }
    suspend fun setMirrorHost(v: String) = context.dataStore.edit { it[KEY_MIRROR_HOST] = v.trim().trimEnd('/') }
    suspend fun setMultiThreadDownload(v: Boolean) = context.dataStore.edit { it[KEY_MULTI_THREAD] = v }
    suspend fun setDownloadThreadCount(v: Int) = context.dataStore.edit { it[KEY_THREAD_COUNT] = v.coerceIn(MIN_THREADS, MAX_THREADS) }
    suspend fun setUseApiForRelease(v: Boolean) = context.dataStore.edit { it[KEY_API_RELEASE] = v }
    suspend fun setPureBlackDarkMode(v: Boolean) = context.dataStore.edit { it[KEY_PURE_BLACK_DARK] = v }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = v }
    suspend fun setGithubToken(v: String) = context.dataStore.edit { it[KEY_GITHUB_TOKEN] = v.trim() }
    suspend fun setWizardCompleted(v: Boolean = true) = context.dataStore.edit { it[KEY_WIZARD_COMPLETED] = v }

    companion object {
        const val DEFAULT_MIRROR_HOST = "https://ghfast.top"
        const val DEFAULT_THREAD_COUNT = 4
        const val MIN_THREADS = 2
        const val MAX_THREADS = 8
        private val KEY_MIRROR_ENABLED = booleanPreferencesKey("mirror_enabled")
        private val KEY_MIRROR_HOST = stringPreferencesKey("mirror_host")
        private val KEY_MULTI_THREAD = booleanPreferencesKey("multi_thread_download")
        private val KEY_THREAD_COUNT = intPreferencesKey("download_thread_count")
        private val KEY_API_RELEASE = booleanPreferencesKey("use_api_release")
        private val KEY_PURE_BLACK_DARK = booleanPreferencesKey("pure_black_dark")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_GITHUB_TOKEN = stringPreferencesKey("github_token")
        private val KEY_WIZARD_COMPLETED = booleanPreferencesKey("wizard_completed")
    }
}

package com.example.allinone.data.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "token_store")

class TokenStore(private val context: Context) {

    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_UID = longPreferencesKey("uid")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_LAST_SYNC = stringPreferencesKey("last_sync_iso")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val uidFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_UID] }

    val nameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_NAME] }
    val lastSyncFlow: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_SYNC] }

    // ✅ 記憶體快取（給 Repository / UI 用，避免 suspend）
    @Volatile
    private var cachedUid: Long? = null

    init {
        // ✅ App 啟動時先 preload 一次 uid 進 cache（只做一次）
        cachedUid = runBlocking { uidFlow.first() }
    }
    suspend fun saveLogin(token: String, uid: Long, name: String) {
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_UID] = uid
            it[KEY_NAME] = name
        }
        // ✅ 同步更新 cache
        cachedUid = uid
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        // ✅ 清 cache
        cachedUid = null
    }

    suspend fun setLastSyncIso(iso: String) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = iso }
    }

    suspend fun getUserUid(): Long? = uidFlow.first()
    suspend fun getLastSyncIso(): String? = lastSyncFlow.first()
    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun getName(): String? = nameFlow.first()

    /** Worker / Interceptor 偶爾需要同步讀取（簡化用）。 */
    fun getTokenBlocking(): String? = runBlocking { getToken() }
    fun getLastSyncBlocking(): String? = runBlocking { getLastSyncIso() }

    // ✅ 新增：Repository 專用（非 suspend）
    fun peekUserUid(): Long? = cachedUid
}

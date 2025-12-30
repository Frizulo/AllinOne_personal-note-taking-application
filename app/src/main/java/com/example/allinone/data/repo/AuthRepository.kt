package com.example.allinone.data.repo

import com.example.allinone.data.remote.AllInOneApi
import com.example.allinone.data.remote.dto.AuthRequest
import com.example.allinone.data.store.TokenStore
import com.example.allinone.data.remote.dto.ApiErrorResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException

class AuthRepository(
    private val api: AllInOneApi,
    private val tokenStore: TokenStore
) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val errAdapter = moshi.adapter(ApiErrorResponse::class.java)

    suspend fun register(name: String, password: String) {
        try {
            api.register(AuthRequest(name = name, password = password))
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val parsed = body?.let { runCatching { errAdapter.fromJson(it) }.getOrNull() }

            // ✅ 優先顯示 server 的 message；沒有就用代碼/狀態碼兜底
            val msg = parsed?.message
                ?: when (e.code()) {
                    409 -> "此使用者名稱已被註冊"
                    400 -> "請輸入帳號與密碼"
                    else -> "註冊失敗（${e.code()}）"
                }

            throw Exception(msg)
        }
    }

    suspend fun login(name: String, password: String) {
        val resp = api.login(AuthRequest(name = name, password = password))
        tokenStore.saveLogin(resp.token, resp.uid, resp.name)
    }

}

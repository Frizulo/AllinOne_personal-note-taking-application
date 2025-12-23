package com.example.allinone.data.repo

import com.example.allinone.data.remote.AllInOneApi
import com.example.allinone.data.remote.dto.AuthRequest
import com.example.allinone.data.store.TokenStore

class AuthRepository(
    private val api: AllInOneApi,
    private val tokenStore: TokenStore
) {
    suspend fun register(name: String, password: String) {
        api.register(AuthRequest(name = name, password = password))
    }

    suspend fun login(name: String, password: String) {
        val resp = api.login(AuthRequest(name = name, password = password))
        tokenStore.saveLogin(resp.token, resp.uid, resp.name)
    }


//    suspend fun logout() {
//        tokenStore.clear()
//    }
}

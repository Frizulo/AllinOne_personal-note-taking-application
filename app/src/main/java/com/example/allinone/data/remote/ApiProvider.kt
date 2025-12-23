package com.example.allinone.data.remote

import android.content.Context
import com.example.allinone.data.store.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiProvider {

    /**
     * 重要：請把 BASE_URL 改成你的後端 URL
     *  - Android Emulator 連本機： http://10.0.2.2:PORT/api/
     *  - 實機請用同網段 IP： http://192.168.x.x:PORT/api/
     */
    const val BASE_URL = "http://210.240.160.82:9090/api/"
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun create(context: Context, tokenStore: TokenStore): AllInOneApi {
        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.getTokenBlocking()
            val req = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(req)
        }


        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AllInOneApi::class.java)
    }

}

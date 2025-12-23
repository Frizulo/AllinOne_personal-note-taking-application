package com.example.allinone.data.remote

import com.example.allinone.data.remote.dto.*
import retrofit2.http.*

interface AllInOneApi {

    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @GET("tasks")
    suspend fun listTasks(): TasksListResponse

    @GET("sync/pull")
    suspend fun pullChanges(@Query("since") since: String): PullResponse

    @POST("sync/push")
    suspend fun pushChanges(@Body body: PushRequest): PushResponse
}

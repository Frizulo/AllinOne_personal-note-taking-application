package com.example.allinone.data.remote.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface GeoApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): GeoResp
}

data class GeoResp(
    val results: List<GeoResult>?
)

data class GeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
    val timezone: String? = null
)

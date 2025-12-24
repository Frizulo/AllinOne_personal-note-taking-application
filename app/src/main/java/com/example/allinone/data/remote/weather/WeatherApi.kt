package com.example.allinone.data.remote.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
        @Query("timezone") timezone: String = "Asia/Taipei"
    ): WeatherResp
}

data class WeatherResp(
    val current: Current?,
    val daily: Daily?
)

data class Current(
    val temperature_2m: Double?,
    val weather_code: Int?
)

data class Daily(
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?,
    val weather_code: List<Int>?
)

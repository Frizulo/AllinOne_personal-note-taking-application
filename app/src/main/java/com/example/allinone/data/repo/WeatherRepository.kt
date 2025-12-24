package com.example.allinone.data.repo

import com.example.allinone.data.remote.weather.GeoApi
import com.example.allinone.data.remote.weather.WeatherApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class HomeWeather(
    val city: String,
    val tempC: Int,
    val description: String,
    val todayMaxC: Int,
    val todayMinC: Int
)

class WeatherRepository(
    private val geoApi: GeoApi,
    private val weatherApi: WeatherApi
) {

    suspend fun fetchTodayWeatherByCity(lat: Double, lon: Double, cityName: String): HomeWeather {
        val w = weatherApi.forecast(lat = lat, lon = lon, timezone = "Asia/Taipei")

        val temp = w.current?.temperature_2m?.roundToInt() ?: 0
        val code = w.current?.weather_code
        val max = w.daily?.temperature_2m_max?.firstOrNull()?.roundToInt() ?: temp
        val min = w.daily?.temperature_2m_min?.firstOrNull()?.roundToInt() ?: temp

        return HomeWeather(
            city = cityName, // UI 顯示用縣市名
            tempC = temp,
            description = weatherCodeToZh(code),
            todayMaxC = max,
            todayMinC = min
        )
    }

    private fun weatherCodeToZh(code: Int?): String {
        return when (code) {
            0 -> "晴朗"
            1,2,3 -> "多雲"
            45,48 -> "有霧"
            51,53,55 -> "毛毛雨"
            61,63,65 -> "下雨"
            71,73,75 -> "下雪"
            80,81,82 -> "陣雨"
            95 -> "雷雨"
            96,99 -> "強雷雨"
            null -> "未知"
            else -> "天氣代碼 $code"
        }
    }
}

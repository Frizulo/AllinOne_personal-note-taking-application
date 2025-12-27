package com.example.allinone.di

import android.content.Context
import com.example.allinone.data.local.AppDatabase
import com.example.allinone.data.remote.ApiProvider
import com.example.allinone.data.repo.AuthRepository
import com.example.allinone.data.repo.TasksRepository
import com.example.allinone.data.repo.WeatherRepository
import com.example.allinone.data.store.TokenStore
import com.example.allinone.data.repo.ScheduleRepository


object ServiceLocator {

    @Volatile private var tokenStore: TokenStore? = null
    @Volatile private var tasksRepo: TasksRepository? = null
    @Volatile private var authRepo: AuthRepository? = null

    // Schedule REPO ADD
    @Volatile private var scheduleRepo: ScheduleRepository? = null

    fun tokenStore(context: Context): TokenStore =
        tokenStore ?: synchronized(this) {
            tokenStore ?: TokenStore(context.applicationContext).also { tokenStore = it }
        }

    fun database(context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    fun tasksRepository(context: Context): TasksRepository =
        tasksRepo ?: synchronized(this) {
            val ts = tokenStore(context)
            val api = ApiProvider.create(context.applicationContext, ts)
            val dao = database(context).taskDao()
            TasksRepository(dao = dao, api = api, tokenStore = ts)
                .also { tasksRepo = it }
        }

    fun scheduleRepository(context: Context): ScheduleRepository =
        scheduleRepo ?: synchronized(this) {
            val dao = database(context).scheduleDao()
            ScheduleRepository(scheduleDao = dao)
                .also { scheduleRepo = it }
        }

    fun authRepository(context: Context): AuthRepository =
        authRepo ?: synchronized(this) {
            val ts = tokenStore(context)
            val api = ApiProvider.create(context.applicationContext, ts)
            AuthRepository(api = api, tokenStore = ts)
                .also { authRepo = it }
        }

    fun weatherRepository(context: Context): WeatherRepository {
        val geoApi = ApiProvider.createGeoApi(context)
        val weatherApi = ApiProvider.createWeatherApi(context)
        return WeatherRepository(geoApi, weatherApi)
    }

}

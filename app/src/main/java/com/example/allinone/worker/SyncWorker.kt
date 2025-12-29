package com.example.allinone.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.allinone.di.ServiceLocator

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        //Log.d("AUTH", "tokenPrefix=" + (appContext?.take(12) ?: "null"))
        Log.d("SyncWorker", "SyncWorker START")
        return runCatching {
            ServiceLocator.tasksRepository(applicationContext).syncOnce()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}



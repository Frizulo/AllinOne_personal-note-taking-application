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
        Log.d("SyncWorker", "SyncWorker START")
        return runCatching {
            val ts = ServiceLocator.tokenStore(applicationContext)
            val since = ts.getLastSyncIso() ?: "1970-01-01 00:00:00.000"

            val tasksRepo = ServiceLocator.tasksRepository(applicationContext)
            val scheduleRepo = ServiceLocator.scheduleRepository(applicationContext)

            // 先 push（順序通常無所謂，但建議都先 push）
            tasksRepo.pushPending()
            scheduleRepo.pushPending()

            // 再用同一個 since pull（避免漏拉）
            val t1 = tasksRepo.pullIncrementalSince(since)
            val t2 = scheduleRepo.pullIncremental(since)

            // 最後更新 lastSync = 兩者 serverTime 較新的那個
            val m1 = com.example.allinone.data.mapper.parseServerTimeToMillis(t1)
            val m2 = com.example.allinone.data.mapper.parseServerTimeToMillis(t2)
            ts.setLastSyncIso(if (m1 >= m2) t1 else t2)

        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}



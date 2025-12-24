package com.example.allinone.data.repo

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.allinone.data.local.TaskDao
import com.example.allinone.data.local.entities.PendingAction
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.mapper.toEntity
import com.example.allinone.data.mapper.toPushItem
import com.example.allinone.data.remote.AllInOneApi
import com.example.allinone.data.remote.dto.PushRequest
import com.example.allinone.data.store.TokenStore
import kotlinx.coroutines.flow.Flow

class TasksRepository(
    private val dao: TaskDao,
    private val api: AllInOneApi,
    private val tokenStore: TokenStore
) {
    suspend fun createLocalTask(
        title: String,
        detail: String,
        tag: String,
        quadrant: Int,
        progress: Int = 0,
        dueTimeMillis: Long? = null
    ) {
        val uid = tokenStore.getUserUid() ?: return
        val task = TaskEntity(
            userUid = uid,
            serverTid = null,
            title = title,
            detail = detail,
            dueTimeMillis = dueTimeMillis,
            tag = tag,
            quadrant = quadrant,
            progress = progress,
            createdTimeMillis = System.currentTimeMillis(),
            updatedTimeMillis = System.currentTimeMillis(),
            deletedTimeMillis = null,
            pendingAction = PendingAction.CREATE.code,
            localUpdatedMillis = System.currentTimeMillis()
        )
        dao.upsert(task)
    }

    suspend fun updateLocalTask(
        localId: String,
        title: String,
        detail: String,
        tag: String,
        quadrant: Int,
        progress: Int,
        dueTimeMillis: Long?
    ) {
        val old = dao.findByLocalId(localId) ?: return
        val nextAction = when (PendingAction.from(old.pendingAction)) {
            PendingAction.CREATE -> PendingAction.CREATE
            PendingAction.DELETE -> PendingAction.DELETE
            else -> PendingAction.UPDATE
        }
        dao.upsert(
            old.copy(
                title = title,
                detail = detail,
                tag = tag,
                quadrant = quadrant,
                progress = progress,
                dueTimeMillis = dueTimeMillis,
                pendingAction = nextAction.code,
                updatedTimeMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun markDelete(localId: String) {
        val old = dao.findByLocalId(localId) ?: return
        val nextAction = when (PendingAction.from(old.pendingAction)) {
            PendingAction.CREATE -> {
                // 本地新建但未上傳：直接刪除即可
                dao.hardDeleteByLocalId(localId)
                return
            }
            else -> PendingAction.DELETE
        }
        dao.upsert(
            old.copy(
                deletedTimeMillis = System.currentTimeMillis(),
                pendingAction = nextAction.code,
                localUpdatedMillis = System.currentTimeMillis(),
                updatedTimeMillis = System.currentTimeMillis()
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun syncOnce() {
        pushPending()
        pullIncremental()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun pushPending() {
        val uid = tokenStore.getUserUid() ?: error("not logged in")
        val pending = dao.getPending(uid)
        if (pending.isEmpty()) return

        val items = pending.map { it.toPushItem() }
        val resp = api.pushChanges(PushRequest(items))

        // 用 server 回傳結果回填 server uid / updated_time，並清掉 pending
        for (r in resp.results) {
            val local = dao.findByLocalId(r.clientLocalId) ?: continue
            val cleared = local.copy(
                serverTid = r.serverTid ?: local.serverTid,
                updatedTimeMillis = com.example.allinone.data.mapper.parseServerTimeToMillis(r.updatedTime),
                pendingAction = PendingAction.NONE.code,
                localUpdatedMillis = System.currentTimeMillis()
            )
            // 若 server 說已刪除，直接 hard delete
            if (PendingAction.from(local.pendingAction) == PendingAction.DELETE) {
                local.serverTid?.let { dao.hardDeleteByServerTid(it) } ?: dao.hardDeleteByLocalId(local.localId)
            } else {
                dao.upsert(cleared)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun pullIncremental() {
        val since = tokenStore.getLastSyncIso() ?: "1970-01-01T00:00:00Z"
        val resp = api.pullChanges(since)
        tokenStore.setLastSyncIso(resp.serverTime)

        // upsert by serverTid
        for (dto in resp.items) {
            val existing = dao.findByServerTid(dto.tid)
            if (dto.deletedTime != null) {
                existing?.let { dao.hardDeleteByLocalId(it.localId) }
                continue
            }
            val entity = dto.toEntity(existingLocalId = existing?.localId)
            dao.upsert(entity)
        }
    }

    suspend fun observeTasks(query: String): Flow<List<TaskEntity>> {
        val q = query.trim()
        val uid = tokenStore.getUserUid() ?: error("not logged in")
        return if (q.isEmpty()) dao.observeActive(uid) else dao.observeSearch(uid, q)
    }

    //統計
    suspend fun observeActiveTaskCount(): Flow<Int> {
        val uid = tokenStore.getUserUid() ?: error("not logged in")
        return dao.observeActiveTaskCount(uid)
    }

    suspend fun observeTodayTodoCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int> {
        val (start, end) = todayRangeMillis(nowMillis)
        val uid = tokenStore.getUserUid() ?: error("not logged in")
        return dao.observeTodayTodoCount(uid, start, end)
    }

    suspend fun observeTodayTodoTotalCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int> {
        val (start, end) = todayRangeMillis(nowMillis)
        val uid = tokenStore.getUserUid() ?: error("not logged in")
        return dao.observeTodayTodoTotalCount(uid, start, end)
    }

    private fun todayRangeMillis(nowMillis: Long): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = nowMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000 - 1
        return start to end
    }

    fun observeTasksForDay(dayMillis: Long): Flow<List<TaskEntity>> {
        val (start, end) = dayRangeMillis(dayMillis)
        return dao.observeTasksByDueDay(start, end)
    }

    private fun dayRangeMillis(anyMillis: Long): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = anyMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000 - 1
        return start to end
    }

}

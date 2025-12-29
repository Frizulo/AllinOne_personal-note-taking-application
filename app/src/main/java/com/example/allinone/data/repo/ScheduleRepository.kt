package com.example.allinone.data.repo

import com.example.allinone.data.local.ScheduleDao
import com.example.allinone.data.local.ScheduleSlotWithTask
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class ScheduleRepository(
    private val scheduleDao: ScheduleDao
) {

    fun observeSlotsWithTask(uid: Long, dateMillis: Long): Flow<List<ScheduleSlotWithTask>> {
        return scheduleDao.observeSlotsWithTask(uid, dateMillis)
    }

    /**
     * ✅ Analysis / 查詢報表：依期間 + 關鍵字 + 類型查詢（Task / 純行程）
     * - startDate / endDate：dateMillis（當日 00:00），可為 null 表示不限制
     * - keyword：比對 task.title / task.detail / slot.customTitle（空字串等同 null）
     */
    suspend fun querySlotsForAnalysis(
        uid: Long,
        startDate: Long?,
        endDate: Long?,
        keyword: String?,
        includeTask: Boolean,
        includeFree: Boolean
    ): List<ScheduleSlotWithTask> {
        val k = keyword?.trim().orEmpty()
        val normalizedKeyword = if (k.isBlank()) null else k

        // 兩個都沒勾選時，視為全選（避免查不到資料造成困惑）
        val taskFlag = if (includeTask || (!includeTask && !includeFree)) 1 else 0
        val freeFlag = if (includeFree || (!includeTask && !includeFree)) 1 else 0

        return scheduleDao.querySlotsWithTaskForAnalysis(
            uid = uid,
            startDate = startDate,
            endDate = endDate,
            keyword = normalizedKeyword,
            includeTask = taskFlag,
            includeFree = freeFlag
        )
    }


    /**
     * 新增/編輯 slot（不允許重疊、不允許跨日、會自動 normalize dateMillis = start 的當日 00:00）
     */
    suspend fun saveSlot(slot: ScheduleSlotEntity): SaveSlotResult {
        // 1) validate 基本規則
        if (slot.endTimeMillis <= slot.startTimeMillis) {
            return SaveSlotResult.Error("結束時間必須晚於開始時間")
        }

        // 2) normalize dateMillis = startTime 當日 00:00（Local timezone）
        val normalizedDate = normalizeToStartOfDay(slot.startTimeMillis)
        val dayEndExclusive = normalizedDate + DAY_MS

        // 3) 禁止跨日：start/end 必須落在同一天
        if (slot.startTimeMillis < normalizedDate || slot.endTimeMillis > dayEndExclusive) {
            return SaveSlotResult.Error("目前版本不支援跨日排程，請將時段限制在同一天內")
        }

        // 4) customTitle / localTaskId 規則（避免資料狀態混亂）
        if (slot.localTaskId == null) {
            val title = slot.customTitle?.trim().orEmpty()
            if (title.isEmpty()) {
                return SaveSlotResult.Error("未關聯任務時，請輸入時段標題")
            }
        }

        val now = System.currentTimeMillis()
        val finalSlot = slot.copy(
            dateMillis = normalizedDate,
            updatedTimeMillis = now
        )

        // 5) 衝突檢查（排除自己）
        val excludeId = if (finalSlot.slotId == 0L) -1L else finalSlot.slotId
        val conflict = scheduleDao.findFirstConflict(
            uid = finalSlot.ownerUid,
            date = finalSlot.dateMillis,
            newStart = finalSlot.startTimeMillis,
            newEnd = finalSlot.endTimeMillis,
            excludeSlotId = excludeId
        )

        if (conflict != null) {
            return SaveSlotResult.Conflict(conflict)
        }

        // 6) insert / update
        if (finalSlot.slotId == 0L) {
            scheduleDao.insert(finalSlot.copy(createdTimeMillis = now))
        } else {
            scheduleDao.update(finalSlot)
        }

        return SaveSlotResult.Success
    }

    suspend fun softDeleteSlot(uid: Long, slotId: Long) {
        scheduleDao.softDeleteById(uid, slotId, System.currentTimeMillis())
    }

    /**
     * [缺口 4] 快速排程：找當日第一個 1 小時空檔（08:00~22:00）
     * stepMinutes 建議 30（比較符合 UI 操作）
     */
    suspend fun findFirstFreeGapOneHour(
        uid: Long,
        dateMillis: Long,
        stepMinutes: Int = 30
    ): Pair<Long, Long>? {
        val slots = scheduleDao.getSlotsByDate(uid, dateMillis)

        val start = dateMillis + 8L * HOUR_MS
        val endLimit = dateMillis + 22L * HOUR_MS
        val oneHour = 1L * HOUR_MS
        val step = stepMinutes.toLong() * MIN_MS

        var current = start

        for (s in slots) {
            if (s.startTimeMillis - current >= oneHour) {
                return current to (current + oneHour)
            }
            current = max(current, s.endTimeMillis)
            current = alignUp(current, step)
            if (current > endLimit) break
        }

        if (current <= endLimit && (dateMillis + DAY_MS - current) >= oneHour) {
            return current to (current + oneHour)
        }
        return null
    }

    // -------------------------
    // ✅ 4x3 統計（睡眠/早/中/晚）×（Total/Task/Free）
    // - Sleep:     00–06
    // - Morning:   06–12
    // - Afternoon: 12–18
    // - Evening:   18–24
    // -------------------------

    enum class TimeBucket4 { Sleep, Morning, Afternoon, Evening }

    data class ScheduleStats4x3(
        val sleepTotal: Long = 0, val sleepTask: Long = 0, val sleepFree: Long = 0,
        val morningTotal: Long = 0, val morningTask: Long = 0, val morningFree: Long = 0,
        val afternoonTotal: Long = 0, val afternoonTask: Long = 0, val afternoonFree: Long = 0,
        val eveningTotal: Long = 0, val eveningTask: Long = 0, val eveningFree: Long = 0,
    ) {
        fun add(bucket: TimeBucket4, dur: Long, isTask: Boolean): ScheduleStats4x3 {
            fun inc(total: Long, task: Long, free: Long): Triple<Long, Long, Long> =
                if (isTask) Triple(total + dur, task + dur, free)
                else Triple(total + dur, task, free + dur)

            return when (bucket) {
                TimeBucket4.Sleep -> {
                    val (t, tk, fr) = inc(sleepTotal, sleepTask, sleepFree)
                    copy(sleepTotal = t, sleepTask = tk, sleepFree = fr)
                }
                TimeBucket4.Morning -> {
                    val (t, tk, fr) = inc(morningTotal, morningTask, morningFree)
                    copy(morningTotal = t, morningTask = tk, morningFree = fr)
                }
                TimeBucket4.Afternoon -> {
                    val (t, tk, fr) = inc(afternoonTotal, afternoonTask, afternoonFree)
                    copy(afternoonTotal = t, afternoonTask = tk, afternoonFree = fr)
                }
                TimeBucket4.Evening -> {
                    val (t, tk, fr) = inc(eveningTotal, eveningTask, eveningFree)
                    copy(eveningTotal = t, eveningTask = tk, eveningFree = fr)
                }
            }
        }
    }

    fun calculate4x3Stats(dateMillis: Long, slots: List<ScheduleSlotWithTask>): ScheduleStats4x3 {
        val b1 = dateMillis + 6L * HOUR_MS
        val b2 = dateMillis + 12L * HOUR_MS
        val b3 = dateMillis + 18L * HOUR_MS
        val b4 = dateMillis + 24L * HOUR_MS

        fun bucket(t: Long): TimeBucket4 = when {
            t < b1 -> TimeBucket4.Sleep
            t < b2 -> TimeBucket4.Morning
            t < b3 -> TimeBucket4.Afternoon
            else -> TimeBucket4.Evening
        }

        var stats = ScheduleStats4x3()

        for (item in slots) {
            val s = item.slot.startTimeMillis
            val e = item.slot.endTimeMillis
            if (e <= s) continue

            var cur = s
            while (cur < e) {
                val nextBoundary = when {
                    cur < b1 -> b1
                    cur < b2 -> b2
                    cur < b3 -> b3
                    else -> b4
                }
                val segEnd = min(e, nextBoundary)
                val dur = segEnd - cur
                val isTask = item.slot.localTaskId != null

                stats = stats.add(bucket(cur), dur, isTask)
                cur = segEnd
            }
        }

        return stats
    }

    data class AnalysisSummary(
        val totalMs: Long,
        val taskMs: Long,
        val freeMs: Long,
        val stats4x3: ScheduleStats4x3
    )

    /**
     * ✅ 多日統計：把 slots 依 dateMillis 分組，逐日計算 4x3，再加總
     */
    fun summarizeForAnalysis(slots: List<ScheduleSlotWithTask>): AnalysisSummary {
        var total = 0L
        var task = 0L
        var free = 0L
        var stats = ScheduleStats4x3()

        val grouped = slots.groupBy { it.slot.dateMillis }
        for ((day0, daySlots) in grouped) {
            // 總時長
            for (it in daySlots) {
                val dur = (it.slot.endTimeMillis - it.slot.startTimeMillis).coerceAtLeast(0)
                total += dur
                if (it.slot.localTaskId != null) task += dur else free += dur
            }
            // 4x3 分段（處理跨時段切割）
            stats = mergeStats(stats, calculate4x3Stats(day0, daySlots))
        }

        return AnalysisSummary(totalMs = total, taskMs = task, freeMs = free, stats4x3 = stats)
    }

    private fun mergeStats(a: ScheduleStats4x3, b: ScheduleStats4x3): ScheduleStats4x3 =
        ScheduleStats4x3(
            sleepTotal = a.sleepTotal + b.sleepTotal,
            sleepTask = a.sleepTask + b.sleepTask,
            sleepFree = a.sleepFree + b.sleepFree,
            morningTotal = a.morningTotal + b.morningTotal,
            morningTask = a.morningTask + b.morningTask,
            morningFree = a.morningFree + b.morningFree,
            afternoonTotal = a.afternoonTotal + b.afternoonTotal,
            afternoonTask = a.afternoonTask + b.afternoonTask,
            afternoonFree = a.afternoonFree + b.afternoonFree,
            eveningTotal = a.eveningTotal + b.eveningTotal,
            eveningTask = a.eveningTask + b.eveningTask,
            eveningFree = a.eveningFree + b.eveningFree,
        )


    // --------------------
    // Helpers
    // --------------------
    private fun normalizeToStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun alignUp(value: Long, step: Long): Long {
        val mod = value % step
        return if (mod == 0L) value else value + (step - mod)
    }

    companion object {
        private const val MIN_MS = 60_000L
        private const val HOUR_MS = 3_600_000L
        private const val DAY_MS = 86_400_000L
    }
}

// --------------------
// Result types
// --------------------
sealed class SaveSlotResult {
    data object Success : SaveSlotResult()
    data class Error(val message: String) : SaveSlotResult()
    data class Conflict(val conflictSlot: ScheduleSlotEntity) : SaveSlotResult()
}

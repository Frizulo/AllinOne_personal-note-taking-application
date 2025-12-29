package com.example.allinone.data.local

import androidx.room.*
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(slot: ScheduleSlotEntity): Long

    @Update
    suspend fun update(slot: ScheduleSlotEntity)

    @Query("""
        SELECT * FROM schedule_slots
        WHERE ownerUid = :uid AND dateMillis = :date AND deletedTimeMillis IS NULL
        ORDER BY startTimeMillis ASC
    """)
    suspend fun getSlotsByDate(uid: Long, date: Long): List<ScheduleSlotEntity>

    // 衝突對象（支援編輯排除自己）
    @Query("""
        SELECT * FROM schedule_slots 
        WHERE ownerUid = :uid 
          AND dateMillis = :date
          AND deletedTimeMillis IS NULL
          AND startTimeMillis < :newEnd 
          AND :newStart < endTimeMillis
          AND slotId != :excludeSlotId
        ORDER BY startTimeMillis ASC
        LIMIT 1
    """)
    suspend fun findFirstConflict(
        uid: Long,
        date: Long,
        newStart: Long,
        newEnd: Long,
        excludeSlotId: Long = -1
    ): ScheduleSlotEntity?

    // B 視圖：slots + task title/quadrant（LEFT JOIN）
    @Query("""
        SELECT s.*,
               t.title AS taskTitle,
               t.quadrant AS taskQuadrant
        FROM schedule_slots s
        LEFT JOIN tasks t ON s.localTaskId = t.localId
        WHERE s.ownerUid = :uid 
          AND s.dateMillis = :date
          AND s.deletedTimeMillis IS NULL
        ORDER BY s.startTimeMillis ASC
    """)
    fun observeSlotsWithTask(uid: Long, date: Long): Flow<List<ScheduleSlotWithTask>>

    // ✅ Analysis / 查詢報表：依期間 + 關鍵字 + 類型（Task / 純行程）查詢
    // - startDate / endDate 為「dateMillis（當日 00:00）」的範圍（可為 null 表示不限制）
    // - keyword 會比對：task.title / task.detail / slot.customTitle
    @Query(
        """
        SELECT s.*,
               t.title AS taskTitle,
               t.quadrant AS taskQuadrant
        FROM schedule_slots s
        LEFT JOIN tasks t ON s.localTaskId = t.localId
        WHERE s.ownerUid = :uid
          AND s.deletedTimeMillis IS NULL
          AND (:startDate IS NULL OR s.dateMillis >= :startDate)
          AND (:endDate IS NULL OR s.dateMillis <= :endDate)
          AND (
                :keyword IS NULL OR :keyword = ''
                OR (t.title LIKE '%' || :keyword || '%')
                OR (t.detail LIKE '%' || :keyword || '%')
                OR (s.customTitle LIKE '%' || :keyword || '%')
          )
          AND (
                (:includeTask = 1 AND s.localTaskId IS NOT NULL)
                OR (:includeFree = 1 AND s.localTaskId IS NULL)
          )
        ORDER BY s.dateMillis ASC, s.startTimeMillis ASC
        """
    )
    suspend fun querySlotsWithTaskForAnalysis(
        uid: Long,
        startDate: Long?,
        endDate: Long?,
        keyword: String?,
        includeTask: Int,
        includeFree: Int
    ): List<ScheduleSlotWithTask>


    @Query("""
        UPDATE schedule_slots
        SET deletedTimeMillis = :now,
            updatedTimeMillis = :now,
            syncState = 3
        WHERE ownerUid = :uid AND slotId = :slotId AND deletedTimeMillis IS NULL
    """)
    suspend fun softDeleteById(uid: Long, slotId: Long, now: Long)

    // Task soft delete 後：slot 轉純時間管理
    @Query("""
        UPDATE schedule_slots
        SET localTaskId = NULL,
            customTitle = COALESCE(customTitle, :taskTitle),
            updatedTimeMillis = :now
        WHERE ownerUid = :uid
          AND localTaskId = :localTaskId
          AND deletedTimeMillis IS NULL
    """)
    suspend fun detachTaskFromSlots(
        uid: Long,
        localTaskId: String,
        taskTitle: String,
        now: Long
    )
}

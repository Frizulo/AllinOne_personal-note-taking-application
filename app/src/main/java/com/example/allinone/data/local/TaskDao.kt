package com.example.allinone.data.local

import androidx.room.*
import com.example.allinone.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("""
        SELECT * FROM tasks
        WHERE deletedTimeMillis IS NULL AND userUid = :uid
        ORDER BY localUpdatedMillis DESC
    """)
    fun observeActive(uid: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE deletedTimeMillis IS NULL AND userUid = :uid
          AND (
            title LIKE '%' || :q || '%'
            OR detail LIKE '%' || :q || '%'
            OR tag LIKE '%' || :q || '%'
          )
        ORDER BY localUpdatedMillis DESC
    """)
    fun observeSearch(uid: Long, q: String): Flow<List<TaskEntity>>

    @Query("""
    SELECT * FROM tasks
    WHERE deletedTimeMillis IS NULL
      AND dueTimeMillis IS NOT NULL
      AND dueTimeMillis BETWEEN :startMillis AND :endMillis
    ORDER BY dueTimeMillis ASC, localUpdatedMillis DESC
""")
    fun observeTasksByDueDay(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE deletedTimeMillis IS NULL AND userUid = :uid
          AND progress != 2
          AND dueTimeMillis IS NOT NULL
          AND dueTimeMillis BETWEEN :startMillis AND :endMillis
    """)
    fun observeTodayTodoCount(uid: Long, startMillis: Long, endMillis: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE deletedTimeMillis IS NULL AND userUid = :uid
          AND dueTimeMillis IS NOT NULL
          AND dueTimeMillis BETWEEN :startMillis AND :endMillis
    """)
    fun observeTodayTodoTotalCount(uid: Long, startMillis: Long, endMillis: Long): Flow<Int>

    // 2) 未完成總數（不含完成、不含刪除）
    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE deletedTimeMillis IS NULL AND userUid = :uid
          AND progress != 2
    """)
    fun observeActiveTaskCount(uid: Long): Flow<Int>

    @Query("""
        SELECT * FROM tasks 
        WHERE pendingAction != 0 AND userUid = :uid
        ORDER BY localUpdatedMillis ASC
    """)
    suspend fun getPending(uid: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE localId = :localId")
    suspend fun hardDeleteByLocalId(localId: String)

    @Query("DELETE FROM tasks WHERE serverTid = :serverTid")
    suspend fun hardDeleteByServerTid(serverTid: Long)

    @Query("SELECT * FROM tasks WHERE serverTid = :serverTid LIMIT 1")
    suspend fun findByServerTid(serverTid: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE localId = :localId LIMIT 1")
    suspend fun findByLocalId(localId: String): TaskEntity?


    //之後新增功能用12
    @Query("DELETE FROM tasks WHERE userUid != :uid")
    suspend fun clearOtherUsers(uid: Long)
}

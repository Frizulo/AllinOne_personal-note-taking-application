package com.example.allinone.data.local.entities

import androidx.room.*

@Entity(
    tableName = "schedule_slots",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["localId"],
            childColumns = ["localTaskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["ownerUid", "dateMillis"]),
        Index(value = ["localTaskId"])
    ]
)
data class ScheduleSlotEntity(
    @PrimaryKey(autoGenerate = true) val slotId: Long = 0,

    val ownerUid: Long,        // 多帳號隔離（對齊 TokenStore 的 uid: Long）
    val dateMillis: Long,      // 當日 00:00（會在 Repository normalize）
    val startTimeMillis: Long,
    val endTimeMillis: Long,

    val localTaskId: String? = null,   // 對齊 TaskEntity.localId (String UUID)
    val customTitle: String? = null,   // 若未關聯 task 或 task 被刪
    val note: String? = null,

    // 未來雲端預留（本版可先不用）
    val serverSlotId: Long? = null,
    val syncState: Int = 0, // 0=Synced, 1=PendingCreate, 2=PendingUpdate, 3=PendingDelete

    val createdTimeMillis: Long = System.currentTimeMillis(),
    val updatedTimeMillis: Long = System.currentTimeMillis(),
    val deletedTimeMillis: Long? = null
)

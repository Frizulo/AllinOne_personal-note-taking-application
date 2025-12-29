package com.example.allinone.data.remote.dto

data class ScheduleSyncPushRequest(
    val items: List<ScheduleSyncPushItem>
)

data class ScheduleSyncPushItem(
    val op: String,              // "create" | "update" | "delete"
    val clientSlotId: Long,       // local slotId
    val serverSlotId: Long?,      // null for create
    val dateMillis: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val localTaskId: String?,
    val customTitle: String?,
    val note: String?,
    val updatedTime: String       // 用 server time format（你 task 用的是 String）
)

data class ScheduleSyncPushResponse(
    val serverTime: String,
    val results: List<ScheduleSyncPushResult>
)

data class ScheduleSyncPushResult(
    val clientSlotId: Long,
    val serverSlotId: Long?,
    val updatedTime: String,
    val deleted: Boolean = false
)

data class ScheduleSyncPullResponse(
    val serverTime: String,
    val items: List<ScheduleSyncSlotDto>
)

data class ScheduleSyncSlotDto(
    val serverSlotId: Long,
    val ownerUid: Long,
    val dateMillis: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val localTaskId: String?,
    val customTitle: String?,
    val note: String?,
    val updatedTime: String,
    val deletedTime: String? = null
)

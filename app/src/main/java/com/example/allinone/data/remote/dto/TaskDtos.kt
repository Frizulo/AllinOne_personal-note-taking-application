package com.example.allinone.data.remote.dto

import com.squareup.moshi.Json

data class TaskDto(
    @Json(name = "tid") val tid: Long,
    @Json(name = "user_uid") val userUid: Long,
    val title: String,
    val detail: String,
    @Json(name = "due_time") val dueTime: String?,
    val tag: String,
    val quadrant: Int,
    val progress: Int,
    @Json(name = "created_time") val createdTime: String? = null,
    @Json(name = "updated_time") val updatedTime: String,
    @Json(name = "deleted_time") val deletedTime: String?
)

data class TasksListResponse(
    val items: List<TaskDto>
)

data class PullResponse(
    @Json(name = "server_time") val serverTime: String,
    val items: List<TaskDto>
)

data class PushRequest(
    val items: List<PushItem>
)

data class PushItem(
    @Json(name = "client_local_id") val clientLocalId: String,
    @Json(name = "server_tid") val serverTid: Long?,
    val title: String,
    val detail: String,
    @Json(name = "due_time") val dueTime: String?,
    val tag: String,
    val quadrant: Int,
    val progress: Int,
    @Json(name = "updated_time") val updatedTime: String?,
    @Json(name = "is_deleted") val isDeleted: Boolean
)

data class PushResponse(
    val results: List<PushResult>
)

data class PushResult(
    @Json(name = "client_local_id") val clientLocalId: String,
    @Json(name = "server_tid") val serverTid: Long?,
    @Json(name = "updated_time") val updatedTime: String,
    val status: String
)

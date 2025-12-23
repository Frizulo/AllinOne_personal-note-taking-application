package com.example.allinone.data.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.allinone.data.local.entities.PendingAction
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.remote.dto.TaskDto
import com.example.allinone.data.remote.dto.PushItem


@RequiresApi(Build.VERSION_CODES.O)
fun TaskDto.toEntity(existingLocalId: String? = null): TaskEntity {
    return TaskEntity(
        localId = existingLocalId ?: java.util.UUID.randomUUID().toString(),
        userUid = userUid,
        serverTid = tid,
        title = title,
        detail = detail,
        dueTimeMillis = dueTime?.let(::parseServerTimeToMillis),
        tag = tag,
        quadrant = quadrant,
        progress = progress,
        createdTimeMillis = createdTime?.let(::parseServerTimeToMillis),
        updatedTimeMillis = parseServerTimeToMillis(updatedTime),
        deletedTimeMillis = deletedTime?.let(::parseServerTimeToMillis),
        pendingAction = PendingAction.NONE.code,
        localUpdatedMillis = System.currentTimeMillis()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun TaskEntity.toPushItem(): PushItem {
    return PushItem(
        clientLocalId = localId,
        serverTid = serverTid,
        title = title,
        detail = detail,
        dueTime = dueTimeMillis?.let(::normalizeToLocalStartOfDay),
        tag = tag,
        quadrant = quadrant,
        progress = progress,
        updatedTime = updatedTimeMillis?.let(::millisToServerIso),
        isDeleted = deletedTimeMillis != null || PendingAction.from(pendingAction) == PendingAction.DELETE
    )
}

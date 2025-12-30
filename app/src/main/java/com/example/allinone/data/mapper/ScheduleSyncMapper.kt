package com.example.allinone.data.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import com.example.allinone.data.remote.dto.ScheduleSyncPushItem
import com.example.allinone.data.remote.dto.ScheduleSyncSlotDto

@RequiresApi(Build.VERSION_CODES.O)
fun ScheduleSlotEntity.toSchedulePushItem(op: String): ScheduleSyncPushItem {
    return ScheduleSyncPushItem(
        op = op,
        clientSlotId = slotId,
        serverSlotId = serverSlotId,
        dateMillis = TaipeiMillisFixToUtcMillis(dateMillis),
        startTimeMillis = TaipeiMillisFixToUtcMillis(startTimeMillis),
        endTimeMillis = TaipeiMillisFixToUtcMillis(endTimeMillis),
        localTaskId = localTaskId,
        customTitle = customTitle,
        note = note,
        updatedTime = millisToServerIso(updatedTimeMillis)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun ScheduleSyncSlotDto.toEntity(existingLocalId: Long?): ScheduleSlotEntity {
    return ScheduleSlotEntity(
        slotId = existingLocalId ?: 0L,
        ownerUid = ownerUid,
        dateMillis = utcMillisFixToTaipeiMillis(dateMillis),
        startTimeMillis = utcMillisFixToTaipeiMillis(startTimeMillis),
        endTimeMillis = utcMillisFixToTaipeiMillis(endTimeMillis),
        localTaskId = localTaskId,
        customTitle = customTitle,
        note = note,
        serverSlotId = serverSlotId,
        syncState = 0,
        createdTimeMillis = existingLocalId?.let { System.currentTimeMillis() } ?: System.currentTimeMillis(),
        updatedTimeMillis = parseServerTimeToMillis(updatedTime),
        deletedTimeMillis = deletedTime?.let(::parseServerTimeToMillis)
    )
}

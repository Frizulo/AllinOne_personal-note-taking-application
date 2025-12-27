package com.example.allinone.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.allinone.data.local.entities.ScheduleSlotEntity

data class ScheduleSlotWithTask(
    @Embedded val slot: ScheduleSlotEntity,
    @ColumnInfo(name = "taskTitle") val taskTitle: String?,
    @ColumnInfo(name = "taskQuadrant") val taskQuadrant: Int?
)

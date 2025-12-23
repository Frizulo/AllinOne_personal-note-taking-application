package com.example.allinone.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),

    val userUid: Long,
    val serverTid: Long?,

    val title: String,
    val detail: String,
    val dueTimeMillis: Long?,
    val tag: String,
    val quadrant: Int,
    val progress: Int, // 0:not yet, 1:in progress, 2:done

    val createdTimeMillis: Long?,
    val updatedTimeMillis: Long?,
    val deletedTimeMillis: Long?,

    val pendingAction: Int = PendingAction.NONE.code,
    val localUpdatedMillis: Long = System.currentTimeMillis()
)

enum class PendingAction(val code: Int) {
    NONE(0),
    CREATE(1),
    UPDATE(2),
    DELETE(3);

    companion object {
        fun from(code: Int): PendingAction =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

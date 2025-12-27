package com.example.allinone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.local.entities.ScheduleSlotEntity


@Database(
    entities = [TaskEntity::class, ScheduleSlotEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 當使用者手機裡是 DB v2，而 App 升級到 v3 時，就跑這段新增表！
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS schedule_slots (
                        slotId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ownerUid INTEGER NOT NULL,
                        dateMillis INTEGER NOT NULL,
                        startTimeMillis INTEGER NOT NULL,
                        endTimeMillis INTEGER NOT NULL,
                        localTaskId TEXT,
                        customTitle TEXT,
                        note TEXT,
                        serverSlotId TEXT,
                        syncState INTEGER NOT NULL DEFAULT 0,
                        createdTimeMillis INTEGER NOT NULL,
                        updatedTimeMillis INTEGER NOT NULL,
                        deletedTimeMillis INTEGER,
                        FOREIGN KEY(localTaskId) REFERENCES tasks(localId) ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_slots_ownerUid_dateMillis ON schedule_slots(ownerUid, dateMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_slots_localTaskId ON schedule_slots(localTaskId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "allinone.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

package com.example.allinone.data.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.*
import java.time.format.DateTimeFormatter



@RequiresApi(Build.VERSION_CODES.O)
private val MYSQL_MILLIS: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Taipei"))

@RequiresApi(Build.VERSION_CODES.O)
fun parseServerTimeToMillis(raw: String): Long {
    // 1) ISO-8601 (e.g. 2025-12-20T08:00:00Z)
    try {
        return OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Exception) { }

    // 2) MySQL datetime(3) string (no timezone)
    return try {
        val ldt = LocalDateTime.parse(raw, MYSQL_MILLIS)
        // Assume server time is UTC+8? Better treat as system default; here use UTC.
        ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        // last resort
        System.currentTimeMillis()
    }
}
@RequiresApi(Build.VERSION_CODES.O)
private const val TAIPEI_OFFSET_MS = 8 * 60 * 60 * 1000L

fun utcMillisFixToTaipeiMillis(utcMillis: Long): Long {
    return utcMillis + TAIPEI_OFFSET_MS
}

@RequiresApi(Build.VERSION_CODES.O)
fun millisToServerIso(millis: Long): String =
    MYSQL_MILLIS.format(Instant.ofEpochMilli(millis))

@RequiresApi(Build.VERSION_CODES.O)
fun normalizeToLocalStartOfDay(millis: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(java.util.Calendar.HOUR_OF_DAY, 15)
    cal.set(java.util.Calendar.MINUTE, 59)
    cal.set(java.util.Calendar.SECOND, 59)
    cal.set(java.util.Calendar.MILLISECOND, 999)
    return cal.timeInMillis.let(::millisToServerIso)
}

package com.example.allinone.ui.schedule

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI 用時間工具（以當天 dateMillis=00:00 為基準）
 * - 30 分鐘步進
 * - 不跨日
 */
object TimeOptions {

    private const val MIN_MS = 60_000L
    private const val DAY_MS = 86_400_000L

    /**
     * 產生一天內所有可選時間點（預設 30 分鐘步進）
     * 回傳的是「offsetMillis」（從 00:00 起算的偏移），例如：
     * 00:00 -> 0
     * 00:30 -> 1_800_000
     * ...
     */
    fun offsets(stepMinutes: Int = 30): List<Long> {
        require(stepMinutes > 0 && 60 % stepMinutes == 0) {
            "stepMinutes must divide 60, e.g. 15/20/30"
        }
        val step = stepMinutes.toLong() * MIN_MS
        val list = ArrayList<Long>((DAY_MS / step).toInt() + 1)
        var cur = 0L
        while (cur < DAY_MS) {
            list.add(cur)
            cur += step
        }
        return list
    }

    /** 把 offsetMillis（從 00:00 起算）轉成 HH:mm */
    fun offsetToLabel(offsetMillis: Long): String {
        val hh = (offsetMillis / (60L * MIN_MS)).toInt()
        val mm = ((offsetMillis / MIN_MS) % 60).toInt()
        return "%02d:%02d".format(hh, mm)
    }

    /** 把 dateMillis(00:00) + offsetMillis -> 絕對時間 millis */
    fun absoluteMillis(dateMillis: Long, offsetMillis: Long): Long {
        return dateMillis + offsetMillis
    }

    /** 絕對時間 millis -> offsetMillis（相對當天 00:00） */
    fun toOffsetMillis(dateMillis: Long, absoluteMillis: Long): Long {
        return absoluteMillis - dateMillis
    }

    /** 絕對時間 millis -> HH:mm（顯示用） */
    fun toLabel(absoluteMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.TAIWAN)
        return sdf.format(Date(absoluteMillis))
    }
}

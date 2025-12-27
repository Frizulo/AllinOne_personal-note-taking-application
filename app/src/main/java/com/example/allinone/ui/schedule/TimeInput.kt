package com.example.allinone.ui.schedule

object TimeInput {

    private const val MIN_MS = 60_000L

    /** 只留數字，最多 4 碼（HHmm） */
    fun toDigits(raw: String): String = raw.filter { it.isDigit() }.take(4)

    /** digits -> 顯示字串（不強制補 0，不亂插入） */
    fun digitsToDisplay(digits: String): String {
        return when (digits.length) {
            0, 1, 2 -> digits
            else -> digits.substring(0, 2) + ":" + digits.substring(2)
        }
    }

    /**
     * digits(可為 3 或 4 碼) -> offsetMillis
     * - allowStepMinutes = null 代表不限制步進（mm 00~59 都可）
     * - allowStepMinutes = 30 代表只能 00 或 30
     */
    fun digitsToOffsetMillis(digits: String, allowStepMinutes: Int? = 30): Long? {
        val d = digits
        if (d.length < 3) return null

        val padded = d.padStart(4, '0') // 930 -> 0930
        val hh = padded.substring(0, 2).toIntOrNull() ?: return null
        val mm = padded.substring(2, 4).toIntOrNull() ?: return null

        if (hh !in 0..23) return null
        if (mm !in 0..59) return null

        // 最晚 23:59 也可；但若你要限制到 23:30，下面會擋
        if (hh == 23 && mm > 30 && allowStepMinutes == 30) return null

        // 步進限制（可關）
        if (allowStepMinutes != null) {
            if (mm % allowStepMinutes != 0) return null
        }

        // 若你希望最晚只能 23:30（配合 30 分步進）
        if (allowStepMinutes == 30 && hh == 23 && mm == 30) {
            // OK
        } else if (allowStepMinutes == 30 && hh == 23 && mm > 30) {
            return null
        }

        return (hh * 60L + mm) * MIN_MS
    }
}

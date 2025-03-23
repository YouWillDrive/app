package ru.gd_alt.youwilldrive

import java.time.LocalDateTime
import java.time.ZoneOffset

class UtilsProvider {
    companion object {
        fun timestampToDate(timestamp: Int): LocalDateTime {
            return LocalDateTime.ofEpochSecond(timestamp.toLong(), 0, ZoneOffset.ofHours(3))
        }

        fun dateToTimestamp(date: LocalDateTime): Int {
            return date.toEpochSecond(ZoneOffset.ofHours(3)).toInt()
        }
    }
}
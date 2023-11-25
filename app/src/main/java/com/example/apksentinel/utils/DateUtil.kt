package com.example.apksentinel.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtil {

    private const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"
    private const val DEFAULT_TIME_FORMAT = "HH:mm:ss"

    fun formatDate(timestamp: Long, dateFormat: String = DEFAULT_DATE_FORMAT, timeFormat: String = DEFAULT_TIME_FORMAT): String {
        val sdfDate = SimpleDateFormat(dateFormat, Locale.getDefault())
        val sdfTime = SimpleDateFormat(timeFormat, Locale.getDefault())

        val date = Date(timestamp)
        return "${sdfDate.format(date)} ${sdfTime.format(date)}"
    }

    fun parseDate(dateStr: String, format: String = DEFAULT_DATE_FORMAT): Long? {
        return try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            val date = sdf.parse(dateStr)
            date?.time
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
}


package com.example.apksentinel.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtil {

    private const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"

    fun formatDate(timestamp: Long, format: String = DEFAULT_DATE_FORMAT): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(Date(timestamp))
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


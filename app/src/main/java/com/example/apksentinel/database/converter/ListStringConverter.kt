package com.example.apksentinel.database.converter

import androidx.room.TypeConverter

object ListStringConverter {

    @TypeConverter
    @JvmStatic
    fun fromString(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }

    @TypeConverter
    @JvmStatic
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}

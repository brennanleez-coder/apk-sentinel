package com.example.apksentinel.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apks")
data class ApkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val appName: String,
    val packageName: String,
    val appIcon: String,
    val versionName: String?,
    val versionCode: Int,
    val installDate: Long,
    val lastUpdateDate: Long,
    val permissions: Array<String>?,
    val isSystemApp: Boolean,
    val appHash: String
)

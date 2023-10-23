package com.example.apksentinel.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "installed_apks",indices = [Index(value = ["packageName"], unique = true)])
data class ApkItem(
    @PrimaryKey(autoGenerate = true)
    @NotNull
    val id: Long = 0L,
    val appName: String,
    val packageName: String,
    val appIcon: String,
    val versionName: String?,
    val versionCode: Int,
    val installDate: Long,
    val lastUpdateDate: Long,
    val permissions: List<String>,
    val isSystemApp: Boolean,
    val appHash: String,
    val appCertHash: String,
    var isDeleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

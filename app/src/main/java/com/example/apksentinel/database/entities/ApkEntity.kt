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
    var appName: String,
    var packageName: String,
    var appIcon: String,
    val versionName: String?,
    val versionCode: Int,
    var installDate: Long,
    var lastUpdateDate: Long,
    var permissions: List<String>,
    var isSystemApp: Boolean,
    var appHash: String,
    var appCertHash: String,
    var isDeleted: Boolean,
    var timestamp: Long = System.currentTimeMillis()
)

package com.example.apksentinel.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "apk_change_log",indices = [Index(value = ["packageName"], unique = true)])
data class ApkChangeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val oldAppCertHash: String,
    val newAppCertHash: String,
    val permissionsAdded: List<String>,
    val permissionsRemoved: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

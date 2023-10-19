package com.example.apksentinel.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "apk_change_log",indices = [Index(value = ["packageName"], unique = true)])
data class ApkChangeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "old_app_cert_hash")
    val oldAppCertHash: String,

    @ColumnInfo(name = "new_app_cert_hash")
    val newAppCertHash: String,

    @ColumnInfo(name = "permissions_added")
    val permissionsAdded: List<String>,

    @ColumnInfo(name = "permissions_removed")
    val permissionsRemoved: List<String>,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

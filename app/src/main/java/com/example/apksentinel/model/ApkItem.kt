package com.example.apksentinel.model

import android.graphics.drawable.Drawable

//@Parcelize
data class ApkItem(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable,
    val versionName: String?,
    val versionCode: Int,
    val installDate: Long,
    val lastUpdateDate: Long,
    val permissions: Array<String>?,
    val isSystemApp: Boolean,
    val appHash: String,
    val appCertHash: String,
    val isDeleted: Boolean
)
//    : Parcelable

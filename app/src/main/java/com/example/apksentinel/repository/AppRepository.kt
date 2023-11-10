package com.example.apksentinel.repository


import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.model.AppPermissionCount

class AppRepository(private val apkItemDao: ApkItemDao) {

    suspend fun getSystemAppsCount(): Int {
        return apkItemDao.countSystemApps()
    }

    suspend fun getNonSystemAppsCount(): Int {
        return apkItemDao.countNonSystemApps()
    }

    suspend fun getAppsByPermissionCount(): List<AppPermissionCount> {
        return apkItemDao.getAppsByPermissionCount()
    }

    // Other data operations...
}

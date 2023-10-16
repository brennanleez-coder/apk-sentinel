package com.example.apksentinel.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apksentinel.database.entities.ApkItem
import com.example.apksentinel.model.AppPermissionCount
import kotlinx.coroutines.flow.Flow


@Dao
interface ApkItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(apkItem: ApkItem): Long

    //when using corouines, dont use suspend
    @Query("SELECT * FROM installed_apks")
    fun getAllApkItems(): Flow<List<ApkItem>>

    @Query("SELECT * FROM installed_apks WHERE id = :apkId")
    fun getApkItemById(apkId: Long): ApkItem

    @Query("SELECT COUNT(*) FROM installed_apks")
    fun getCount(): Int

    @Query("SELECT COUNT(*) FROM installed_apks WHERE isSystemApp = 1")
    fun countSystemApps(): Int

    @Query("SELECT COUNT(*) FROM installed_apks WHERE isSystemApp = 0")
    fun countNonSystemApps(): Int

    @Query("SELECT appName, LENGTH(permissions) - LENGTH(REPLACE(permissions, ',', '')) + 1 as permissionsCount FROM installed_apks ORDER BY permissionsCount DESC")
    fun getAppsByPermissionCount(): List<AppPermissionCount>
    //The difference between the original string length and the length after removing the commas = total number of commas.
    //Since the number of items in a comma-separated list is always one more than the number of commas, you add 1 to the count of commas to get the count of permissions.


}

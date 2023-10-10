package com.example.apksentinel.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apksentinel.database.entities.ApkItem
import kotlinx.coroutines.flow.Flow


@Dao
interface ApkItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(apkItem: ApkItem): Long

    @Query("SELECT * FROM installed_apks")
    fun getAllApkItems(): Flow<List<ApkItem>>

    @Query("SELECT * FROM installed_apks WHERE id = :apkId")
    fun getApkItemById(apkId: Long): ApkItem


}

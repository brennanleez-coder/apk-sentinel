package com.example.apksentinel.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apksentinel.database.entities.ApkChangeLogEntity

@Dao
interface ApkChangeLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ApkChangeLogEntity)

    @Query("SELECT * FROM apk_change_log")
    fun getAll(): List<ApkChangeLogEntity>

    // Add other methods as needed
}

package com.example.apksentinel.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.apksentinel.database.dao.ApkChangeLogDao
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.database.entities.ApkChangeLogEntity
import com.example.apksentinel.database.entities.ApkItem
import com.example.apksentinel.utils.DbTypeConverter


@Database(entities = [ApkItem::class, ApkChangeLogEntity::class], version = 4, exportSchema = false)
@TypeConverters(DbTypeConverter::class)
abstract class ApkItemDatabase : RoomDatabase() {
    abstract fun apkItemDao(): ApkItemDao
    abstract fun apkChangeLogDao(): ApkChangeLogDao

    companion object {
        @Volatile
        private var INSTANCE: ApkItemDatabase? = null

        fun getDatabase(context: Context): ApkItemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApkItemDatabase::class.java,
                    "apk_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }

}

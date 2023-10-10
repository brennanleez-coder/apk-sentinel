package com.example.apksentinel

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase

class ApkSentinel : Application() {

    lateinit var apkItemDatabase: ApkItemDatabase

    override fun onCreate() {
        super.onCreate()
        // Start the ApkInstallService
        val serviceIntent = Intent(this, ApkInstallService::class.java)
        startService(serviceIntent)
        Log.d("ApkSentinel", "Application Listener created!")

//        apkItemDatabase = Room.databaseBuilder(this, ApkItemDatabase::class.java, "apk_item_database")
//            .fallbackToDestructiveMigration().build()
//        Log.d("ApkSentinel", "${apkItemDatabase} created!")
    }
}

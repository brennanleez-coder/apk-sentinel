package com.example.apksentinel

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class ApkInstallService : Service() {
    private val apkInstallReceiver: BroadcastReceiver = ApkInstallReceiver()

    override fun onCreate() {
        super.onCreate()

        // Register the BroadcastReceiver
//        val filter = IntentFilter()
//        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
//        filter.addDataScheme("package")
//        registerReceiver(apkInstallReceiver, filter)
        Log.d("ApkSentinel", "Service registered!")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the BroadcastReceiver
        unregisterReceiver(apkInstallReceiver)

    }
}

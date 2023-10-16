package com.example.apksentinel

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder


class ApkInstallService : Service() {
    private val apkInstallReceiver: BroadcastReceiver = ApkInstallReceiver()

    override fun onCreate() {
        super.onCreate()

//         Register the BroadcastReceiver
        val filter = IntentFilter()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        registerReceiver(apkInstallReceiver, intentFilter)

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

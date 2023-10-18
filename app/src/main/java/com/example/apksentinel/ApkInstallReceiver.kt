package com.example.apksentinel

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.PACKAGE_ADDED") {
            val packageName = intent.data?.schemeSpecificPart
            val pm = context.packageManager
            try {
                val packageInfo = pm.getPackageInfo(packageName!!, 0)
                val version = packageInfo.versionName
//                sendNotification(context, packageName, version)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }


}
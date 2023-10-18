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
                sendNotification(context, packageName, version)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNotification(context: Context, packageName: String, version: String) {
        val notificationId = 101 // Just a random unique ID

        val builder = NotificationCompat.Builder(context, "YOUR_CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your own icon
            .setContentTitle("App Installed")
            .setContentText("$packageName $version installed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(notificationId, builder.build())
    }
}
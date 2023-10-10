package com.example.apksentinel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log

class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.encodedSchemeSpecificPart
            val message = "Package installed: $packageName"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            Log.d("ApkInstallReceiver", "Package installed: $packageName")
            Log.d("ApkSentinel", "Package installed: $packageName")
        }
    }
}
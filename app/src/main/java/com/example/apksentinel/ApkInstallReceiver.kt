package com.example.apksentinel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.apksentinel.utils.NotificationUtil


class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val packageName = intent.data?.encodedSchemeSpecificPart
        Log.d("Apk Sentinel", "Receiver registered")

        when(intent.action) {
            "android.intent.action.PACKAGE_ADDED" -> {
                NotificationUtil.sendNotification(context!!, "New App Installed", "$packageName has been installed.")
            }
            "android.intent.action.PACKAGE_REMOVED" -> {
                NotificationUtil.sendNotification(context!!, "App Uninstalled", "$packageName has been uninstalled.")
            }
            "android.intent.action.PACKAGE_REPLACED" -> {
                NotificationUtil.sendNotification(context!!, "App Updated", "$packageName has been updated.")
            }
            "android.intent.action.QUERY_PACKAGE_RESTART" -> {
                // App is about to be restarted
                Log.i("Apk Sentinel", "$packageName will be restarted.")
            }
        }
    }

    companion object {
        fun newInstance(): ApkInstallReceiver {
            return ApkInstallReceiver()
        }
    }

}
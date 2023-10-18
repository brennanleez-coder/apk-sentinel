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
        Log.d("Apk Sentinel", "receiver registered")
        if (intent.action == "android.intent.action.PACKAGE_ADDED") {

        } else if (intent.action == "android.intent.action.PACKAGE_REMOVED") {

            val uninstallIntent = Intent()

            uninstallIntent.action = "Uninstall"
            uninstallIntent.putExtra("packageName", packageName)
            NotificationUtil.sendNotification(context!!, "Installation detected", "$packageName uninstalled")
        } else if (intent.action == "android.intent.action.PACKAGE_REPLACED") {
            // Application Replaced
//            toastMessage = "PACKAGE_REPLACED: " + intent.getData().toString() + getApplicationName(context, intent.getData().toString(), PackageManager.GET_UNINSTALLED_PACKAGES);
        } else if (intent.action == "android.intent.action.QUERY_PACKAGE_RESTART") {
        }
    }
    companion object {
        fun newInstance(): ApkInstallReceiver {
            return ApkInstallReceiver()
        }
    }

}
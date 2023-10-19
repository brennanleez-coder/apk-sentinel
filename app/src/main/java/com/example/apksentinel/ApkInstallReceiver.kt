package com.example.apksentinel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.utils.HashUtil
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
                val apkPath = context!!.packageManager.getPackageInfo(packageName!!, 0).applicationInfo.sourceDir
                val newHash = HashUtil.getSHA256HashOfFile(apkPath)

                if (!isHashSameForNewVersion(context, packageName, newHash)) {
                    NotificationUtil.sendNotification(context, "App Updated", "$packageName has been updated and the APK hash has changed.")
                } else {
                    NotificationUtil.sendNotification(context, "App Updated", "$packageName has been updated but the APK hash remains the same.")
                }
            }
            "android.intent.action.QUERY_PACKAGE_RESTART" -> {
                Log.i("Apk Sentinel", "$packageName will be restarted.")
            }
        }
    }
    private fun isHashSameForNewVersion(context: Context, packageName: String, newHash: String): Boolean {
        val database = ApkItemDatabase.getDatabase(context)
        val apkItemDao = database.apkItemDao()
        val existingHash = apkItemDao.getHashForApp(packageName)

        return existingHash == newHash
    }


    companion object {
        fun newInstance(): ApkInstallReceiver {
            return ApkInstallReceiver()
        }
    }

}
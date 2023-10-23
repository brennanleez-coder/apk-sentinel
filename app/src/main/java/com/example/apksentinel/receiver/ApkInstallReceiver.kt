package com.example.apksentinel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.utils.HashUtil
import com.example.apksentinel.utils.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ApkInstallReceiver : BroadcastReceiver() {

    private lateinit var apkItemDao: ApkItemDao

    override fun onReceive(context: Context?, intent: Intent) {
        val packageName = intent.data?.encodedSchemeSpecificPart


        val database = ApkItemDatabase.getDatabase(context!!)
        val apkItemDao = database.apkItemDao()

        Log.d("Apk Sentinel", "Receiver registered. Package Name: $packageName, Intent: ${intent.action}")

        when(intent.action) {
            "android.intent.action.PACKAGE_ADDED" -> {
                NotificationUtil.sendNotification(context, "New App Installed", "$packageName has been installed.")
            }
            "android.intent.action.PACKAGE_REMOVED" -> {
                NotificationUtil.sendNotification(context!!, "App Uninstalled", "$packageName has been uninstalled.")


                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val apkItem = packageName?.let { apkItemDao.getApkItemByPackageName(it) }

                        apkItem?.let {
                            it.isDeleted = true
                            apkItemDao.updateApkItem(it)
                            Log.d("Apk Sentinel", "Apk Entity ID:${apkItem.id} isDeleted has been updated, to ${it.isDeleted}")
                        }
                    } catch (e: Exception) {
                        Log.d("Apk Sentinel", "Error message: ${e.message}")
                    }
                }
            }
            "android.intent.action.PACKAGE_REPLACED" -> {
                //If package replaced has a different signing cert as compared to the previous update, android will block installation by default
                val apkPath = context.packageManager.getPackageInfo(packageName!!, 0).applicationInfo.sourceDir
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
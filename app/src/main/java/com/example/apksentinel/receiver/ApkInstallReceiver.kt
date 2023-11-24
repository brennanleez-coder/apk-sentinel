package com.example.apksentinel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.database.entities.ApkItem
import com.example.apksentinel.utils.HashUtil
import com.example.apksentinel.utils.HttpUtil
import com.example.apksentinel.utils.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


class ApkInstallReceiver : BroadcastReceiver() {

    private lateinit var permissions: String
    private lateinit var apkPath: String
    private lateinit var appHash: String
    private lateinit var appCertHash: String
    private lateinit var versionName: String
    private var versionCode by Delegates.notNull<Int>()

    override fun onReceive(context: Context?, intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            handleIntent(context, intent)
        }
    }

    private fun handleIntent(context: Context?, intent: Intent) {
        if (context == null) {
            return
        }
        val packageName = intent.data?.encodedSchemeSpecificPart
        val action = intent.action


        val database = ApkItemDatabase.getDatabase(context)
        val apkItemDao = database.apkItemDao()

        // Just to test

        if (packageName != null) {


            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                val packageManager = context.packageManager
                val packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                apkPath = packageInfo.applicationInfo.sourceDir
                appHash = HashUtil.hashApkWithSHA256(apkPath)
                appCertHash = packageInfo?.signatures?.get(0)?.toCharsString().toString()
                versionName = packageInfo.versionName
                versionCode = packageInfo.versionCode

                if (packageInfo.requestedPermissions != null) {
                    permissions = packageInfo.requestedPermissions.toList().joinToString(",")
                } else {
                    permissions = ""
                }


                val jsonBody = """
            {
                "package_name": $packageName,
                "incoming_apk_hash": $appHash,
                "incoming_app_cert_hash": ${appCertHash},
                "incoming_permissions": $permissions
            }
            """.trimIndent()
                Log.d("NETWORKCALL", "Response: ${packageInfo?.signatures?.get(0)?.toCharsString().toString()}")

                Log.d("NETWORKCALL", "Response: ${packageInfo?.signatures?.get(0)?.toCharsString()}")

                Log.d("NETWORKCALL", "Response: ${packageInfo?.signatures?.get(0)}")

                Log.d("NETWORKCALL", "Response: ${packageInfo?.signatures}")


                try {
                    val response = HttpUtil.post("http://10.0.2.2:8000/submit_apk", jsonBody)
                    Log.d("NETWORKCALL", "Response: $response")
                } catch (e: Exception) {
                    Log.e("NETWORK CALL ERROR", "Exception", e)
                }

                try {
                    when (action) {
                        Intent.ACTION_PACKAGE_REPLACED -> {
                            /* If package replaced has a different signing cert as compared to the previous update,
                                *  android will block installation by default
                                *  No action needed
                                */


                        }


                        Intent.ACTION_PACKAGE_ADDED -> {/* Listen to app installation
                            *  CASE 1: Fresh installation
                            *  CASE 2: Re-installation
                            */

                            var apkRetrieved = apkItemDao.getApkItemByPackageName(packageName)

                            if (apkRetrieved != null) {
                                //Reinstallation
                                handleReinstallation(apkRetrieved, apkItemDao)
                            } else {
                                //Fresh Installation
                                handleFreshInstallation(apkItemDao, context, packageName)
                            }

                            NotificationUtil.sendNotification(
                                context,
                                "New App Installation",
                                "$packageName's App Cert might not be trusted"
                            )
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("Apk Sentinel", "$context: Package Name Not Found")
                } catch (exception: Exception) {
                    Log.e("Capture Install intent", "$exception")
                }
            }
        }
        if (action == Intent.ACTION_PACKAGE_REMOVED) {
            if (packageName != null && context != null) {
                handlePackageRemoved(context, packageName, apkItemDao)
            }
        }
        if (action == Intent.ACTION_PACKAGE_RESTARTED) {
            handlePackageRestart(packageName)
        }
    }

    private fun handlePackageRestart(packageName: String?) {
        Log.i("Apk Sentinel", "$packageName will be restarted.")
    }

    private fun handleReinstallation(
        apkRetrieved: ApkItem, apkItemDao: ApkItemDao
    ) {
        val isSamePermissions = permissions?.equals(apkRetrieved.permissions) ?: false
        val isSameAppHash = appHash == apkRetrieved.appHash
        val isSameAppCertHash = appCertHash == apkRetrieved.appCertHash
        val isSameVersionName = versionName == apkRetrieved.versionName
        val isSameVersionCode = versionCode == apkRetrieved.versionCode
        val conditions = listOf(
            isSamePermissions,
            isSameAppHash,
            isSameAppCertHash,
            isSameVersionName,
            isSameVersionCode
        )
        if (conditions.all { it }) {/*Trigger Backend component
                                *
                                *
                                */
        } else {
            val newApkEntity = ApkItem(
                appName = apkRetrieved.appName,
                packageName = apkRetrieved.packageName,
                appIcon = apkRetrieved.appIcon,
                versionCode = versionCode,
                versionName = versionName,
                installDate = System.currentTimeMillis(),
                lastUpdateDate = System.currentTimeMillis(),
                permissions = permissions.split(",").filter { it.isNotEmpty() },
                isSystemApp = apkRetrieved.isSystemApp,
                appCertHash = appCertHash,
                appHash = appHash,
                isDeleted = false,
                timestamp = System.currentTimeMillis()
            )
            apkItemDao.updateApkItem(newApkEntity)
        }
    }

    private fun handlePackageRemoved(
        context: Context, packageName: String?, apkItemDao: ApkItemDao
    ) {
        NotificationUtil.sendNotification(
            context, "App Uninstalled", "$packageName has been uninstalled."
        )


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apkItem = packageName?.let { apkItemDao.getApkItemByPackageName(it) }

                // Soft Deletion of apkItem
                apkItem?.let {
                    it.isDeleted = true
                    apkItemDao.updateApkItem(it)
                    Log.d(
                        "Apk Sentinel",
                        "Apk:${apkItem.packageName} isDeleted has been updated, to ${it.isDeleted}"
                    )
                }
            } catch (e: Exception) {
                Log.d("Apk Sentinel", "Error message: ${e.message}")
            }
        }
    }

    private fun handleFreshInstallation(
        apkItemDao: ApkItemDao, context: Context?, packageName: String?
    ) {
        if (context == null || packageName == null || apkItemDao == null) {
            return
        }
        //Check if appCertHash has been seen before
        val listOfTrustedAppCertHash: List<String> = apkItemDao.getAllAppCertHash()
        val isTrustedIncomingAppCertHash = listOfTrustedAppCertHash.contains(appCertHash)
        val message: String =
            "$packageName's App Cert is" + if (isTrustedIncomingAppCertHash) "trusted" else "not trusted"

        //Add insertion into change_apk_log

        NotificationUtil.sendNotification(
            context, "New App Installation", "$message"
        )
    }

    companion object {
        fun newInstance(): ApkInstallReceiver {
            return ApkInstallReceiver()
        }
    }

}
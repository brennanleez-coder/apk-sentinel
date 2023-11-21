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

    private lateinit var apkItemDao: ApkItemDao
    private lateinit var permissions: List<String>
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


        val database = ApkItemDatabase.getDatabase(context)
        val apkItemDao = database.apkItemDao()

//      TODO: Wrap in coroutines, notifications still work
//        HttpUtil.get("http://10.0.0.2:8000/")



        when(intent.action) {

//            TODO: HIT THE ENDPOINT WHENEVER A NEW INTENT IS RECEIVED

            //Listen to app installation (fresh installation or reinstallation)
            "android.intent.action.PACKAGE_ADDED" -> {
                try {
                    if (packageName != null) {
                        val packageManager = context.packageManager
                        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                        permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                        apkPath = packageInfo.applicationInfo.sourceDir
                        appHash = HashUtil.hashApk(apkPath, "SHA-256")
                        appCertHash = packageInfo?.signatures?.get(0)?.toCharsString().toString()
                        versionName = packageInfo.versionName
                        versionCode = packageInfo.versionCode


                        var apkRetrieved = apkItemDao.getApkItemByPackageName(packageName)

                        if (apkRetrieved != null) {
                            //Reinstallation

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
                            if (conditions.all { it }) {
                                /*Trigger Backend component
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
                                    permissions = permissions,
                                    isSystemApp = apkRetrieved.isSystemApp,
                                    appCertHash = appCertHash,
                                    appHash = appHash,
                                    isDeleted = false,
                                    timestamp = System.currentTimeMillis()
                                )
                                apkItemDao.updateApkItem(newApkEntity)
                            }
                        } else {
                            //Fresh Installation
                            handleFreshInstallation(apkItemDao, context, packageName)
                        }
                    }
                    NotificationUtil.sendNotification(context, "New App Installation", "$packageName's App Cert might not be trusted")

                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("Apk Sentinel", "$context: Package Name Not Found")
                }catch (exception: Exception) {
                    Log.e("Capture Install intent","$exception")
                }
            }
            "android.intent.action.PACKAGE_REMOVED" -> {
                NotificationUtil.sendNotification(context!!, "App Uninstalled", "$packageName has been uninstalled.")


                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val apkItem = packageName?.let { apkItemDao.getApkItemByPackageName(it) }

                        //Soft Deletion of apkItem
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
                /*If package replaced has a different signing cert as compared to the previous update,
                * android will block installation by default
                * No action needed
                */


//                val apkPath = context.packageManager.getPackageInfo(packageName!!, 0).applicationInfo.sourceDir
//                val newHash = HashUtil.getSHA256HashOfFile(apkPath)
//                if (!isHashSameForNewVersion(context, packageName, newHash)) {
//                    NotificationUtil.sendNotification(context, "App Updated", "$packageName has been updated and the APK hash has changed.")
//                } else {
//                    NotificationUtil.sendNotification(context, "App Updated", "$packageName has been updated but the APK hash remains the same.")
//                }
            }
            "android.intent.action.QUERY_PACKAGE_RESTART" -> {
                Log.i("Apk Sentinel", "$packageName will be restarted.")
            }
        }
    }

    private fun handleFreshInstallation(
        apkItemDao: ApkItemDao,
        context: Context?,
        packageName: String?
    ) {
        if (context == null || packageName == null || apkItemDao == null) {
            return
        }
        //Check if appCertHash has been seen before
        val listOfTrustedAppCertHash: List<String> = apkItemDao.getAllAppCertHash()
        val isTrustedIncomingAppCertHash = listOfTrustedAppCertHash.contains(appCertHash)
        val message: String = "$packageName's App Cert is" + if (isTrustedIncomingAppCertHash) "trusted" else "not trusted"

        //Add insertion into change_apk_log

        NotificationUtil.sendNotification(
            context,
            "New App Installation",
            "$message"
        )
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
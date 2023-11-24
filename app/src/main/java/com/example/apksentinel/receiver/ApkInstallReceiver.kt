package com.example.apksentinel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkChangeLogDao
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.database.entities.ApkChangeLogEntity
import com.example.apksentinel.database.entities.ApkItem
import com.example.apksentinel.model.ApiResponse
import com.example.apksentinel.model.ApkInformation
import com.example.apksentinel.utils.HashUtil
import com.example.apksentinel.utils.HttpUtil
import com.example.apksentinel.utils.ListToStringConverterUtil
import com.example.apksentinel.utils.NotificationUtil
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates


class ApkInstallReceiver : BroadcastReceiver() {

    private lateinit var permissions: String
    private lateinit var apkPath: String
    private lateinit var appHash: String
    private lateinit var appCertHash: String
    private lateinit var versionName: String
    private var versionCode by Delegates.notNull<Int>()
    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent) {
        receiverScope.launch(Dispatchers.IO) {
            handleIntent(context, intent)
        }


    }

    private suspend fun handleIntent(context: Context?, intent: Intent) {
        if (context == null) {
            return
        }
        val packageName = intent.data?.encodedSchemeSpecificPart
        val action = intent.action


        val database = ApkItemDatabase.getDatabase(context)
        val apkItemDao = database.apkItemDao()
        val apkChangeLogDao = database.apkChangeLogDao()

        if (packageName != null) {


            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                val packageManager = context.packageManager
                val packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                apkPath = packageInfo.applicationInfo.sourceDir
                appHash = HashUtil.hashApkWithSHA256(apkPath)
                appCertHash = HashUtil.hashCertWithSHA256(packageName, packageManager)
                versionName = packageInfo.versionName
                versionCode = packageInfo.versionCode
                permissions = if (packageInfo.requestedPermissions != null) {
                    ListToStringConverterUtil.listToString(packageInfo.requestedPermissions.toList())
                } else {
                    ""
                }

                // Send to Backend component to check for Apk Legitimacy
                sendForApkVerification(packageName, context)

                try {
                    when (action) {
                        Intent.ACTION_PACKAGE_REPLACED -> {
                            /* If package replaced has a different signing cert as compared to the previous update,
                                *  android will block installation by default
                                *  No action needed
                                */
                        }
                        Intent.ACTION_PACKAGE_ADDED -> {
                            /* Listen to app installation
                            *  CASE 1: Fresh installation
                            *  CASE 2: Re-installation
                            */
                            val apkRetrieved = apkItemDao.getApkItemByPackageName(packageName)

                            if (apkRetrieved != null) {
                                // Reinstallation
                                handleReinstallation(apkRetrieved, apkItemDao)
                            } else {
                                //Fresh Installation
                                handleFreshInstallation(apkItemDao, apkChangeLogDao, context,
                                    packageName,
                                    appHash,
                                    appCertHash,
                                    // Convert permission string back to List<String>
                                    ListToStringConverterUtil.stringToList(permissions))
                            }
                            withContext(Dispatchers.Main) {
                                NotificationUtil.sendNotification(
                                    context,
                                    "New App Installation",
                                    "$packageName's App Cert might not be trusted"
                                )
                            }

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
            if (packageName != null) {
                handlePackageRemoved(context, packageName, apkItemDao)
            }
        }

    }

    private suspend fun sendForApkVerification(packageName: String, context: Context) {
        val apkInfo = ApkInformation(packageName, appHash, appCertHash, permissions)
        val jsonBody = Gson().toJson(apkInfo)

        try {
            val response = HttpUtil.post("http://10.0.2.2:8000/submit_apk", jsonBody)
            val responseObj = Gson().fromJson(response, ApiResponse::class.java)
            println("Response: $responseObj")
            if (responseObj.status == "success") {
                withContext(Dispatchers.Main) {
                    NotificationUtil.sendNotification(
                        context,
                        "Verifying Apk",
                        "$packageName sent to Sentinel Sight"
                    )
                }
            }
        } catch (e: Exception) {
            println("Exception: ${e.printStackTrace()}")
        }
    }


    private fun handleReinstallation(
        apkRetrieved: ApkItem, apkItemDao: ApkItemDao
    ) {
        val isSamePermissions = permissions?.equals(apkRetrieved.permissions)
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
        if (conditions.all { it!! }) {/*Trigger Backend component
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
                permissions = ListToStringConverterUtil.stringToList(permissions),
                isSystemApp = apkRetrieved.isSystemApp,
                appCertHash = appCertHash,
                appHash = appHash,
                isDeleted = false,
                timestamp = System.currentTimeMillis()
            )
            println("${newApkEntity.packageName} Deletion Status: ${newApkEntity.isDeleted}")
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
        apkItemDao: ApkItemDao,
        apkChangeLogDao: ApkChangeLogDao,
        context: Context?,
        packageName: String?,
        appHash: String,
        appCertHash: String,
        permissions: List<String>
    ) {
        if (context == null || packageName == null) {
            return
        }
        //Check if appCertHash has been seen before
        val listOfTrustedAppCertHash: List<String> = apkItemDao.getAllAppCertHash()
        val isTrustedIncomingAppCertHash = listOfTrustedAppCertHash.contains(this.appCertHash)


        val message: String =
            "$packageName's App Cert is" + if (isTrustedIncomingAppCertHash) "trusted" else "not trusted"

        // First record of respective package name in ApkChangeLog Table
        val changeLogEntity = ApkChangeLogEntity(
            packageName= packageName,
            appHash= appHash,
            oldAppCertHash= appHash,
            permissionsAdded = permissions
        )
        apkChangeLogDao.insert(changeLogEntity)

        NotificationUtil.sendNotification(
            context, "$packageName was installed", message
        )
    }
    companion object {
        fun newInstance(): ApkInstallReceiver {
            return ApkInstallReceiver()
        }
    }

}
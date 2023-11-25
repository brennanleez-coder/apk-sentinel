package com.example.apksentinel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkChangeLogDao
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.database.entities.ApkChangeLogEntity
import com.example.apksentinel.database.entities.ApkItem
import com.example.apksentinel.model.ApiResponse
import com.example.apksentinel.model.ApkInformation
import com.example.apksentinel.utils.DateUtil
import com.example.apksentinel.utils.DrawableUtil
import com.example.apksentinel.utils.HashUtil
import com.example.apksentinel.utils.HttpUtil
import com.example.apksentinel.utils.ListToStringConverterUtil
import com.example.apksentinel.utils.NotificationUtil
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates


class ApkInstallReceiver : BroadcastReceiver() {

    private lateinit var appName: String
    private lateinit var packageName: String
    private lateinit var appIcon: Drawable
    private lateinit var versionName: String
    private var versionCode by Delegates.notNull<Int>()
    private var installDate by Delegates.notNull<Long>()
    private var lastUpdateDate by Delegates.notNull<Long>()
    private lateinit var permissions: String
    private var isSystemApp by Delegates.notNull<Boolean>()
    private lateinit var appHash: String
    private lateinit var appCertHash: String

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent) {
        receiverScope.launch {
            handleIntent(context, intent)
        }


    }

    private suspend fun handleIntent(context: Context?, intent: Intent) {
        if (context == null) {
            return
        }

        packageName = intent.data?.encodedSchemeSpecificPart.toString()
        val action = intent.action


        // When simulating updates, PACKAGE_REMOVED -> PACKAGE_ADDED -> PACKAGE_REPLACED is captured
        // Filter out PACKAGE_REMOVED and PACKAGE_ADDED and only handle PACKAGE_REPLACED
        // ONLY WHEN the package can already be found in PackageManager signifying Updates
        val packageManager = context.packageManager
        println(action)
        val database = ApkItemDatabase.getDatabase(context)
        val apkItemDao = database.apkItemDao()
        val apkChangeLogDao = database.apkChangeLogDao()
        println("apkItemDao: $apkItemDao")
        println("apkCHangeLogDao: $apkChangeLogDao")

        if (packageName != null) {
            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                val packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val apkPath = packageInfo.applicationInfo.sourceDir
                appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
                appIcon = packageManager.getApplicationIcon(packageName)
                appHash = HashUtil.hashApkWithSHA256(apkPath)
                appCertHash = HashUtil.hashCertWithSHA256(packageName, packageManager)
                versionName = packageInfo.versionName
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    packageInfo.versionCode
                }
                installDate = packageInfo.firstInstallTime
                lastUpdateDate = packageInfo.lastUpdateTime
                isSystemApp =
                    (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                permissions = if (packageInfo.requestedPermissions != null) {
                    ListToStringConverterUtil.listToString(packageInfo.requestedPermissions.toList())
                } else {
                    ""
                }

                try {
                    when (action) {
                        Intent.ACTION_PACKAGE_REPLACED -> {
                            /* If package replaced has a different signing cert as compared to the previous update,
                                *  android will block installation by default
                                *  No action needed
                                */
                            val apkItem = packageName?.let { apkItemDao.getApkItemByPackageName(it) }

                            // Soft Deletion of apkItem
                            apkItem?.let {
                                it.isDeleted = false
                                apkItemDao.updateApkItem(it)
                                Log.d(
                                    "Apk Sentinel",
                                    "Apk:${apkItem.packageName} isDeleted has been updated, to ${it.isDeleted}"
                                )
                            }

                        }
                        Intent.ACTION_PACKAGE_ADDED -> {
                            /* Listen to app installation
                            *  CASE 1: Fresh installation
                            *  CASE 2: Re-installation
                            */
                            val apkRetrieved = apkItemDao.getApkItemByPackageName(packageName)
                            if (apkRetrieved != null) {
                                // Reinstallation
                                handleReinstallation(context, apkRetrieved, apkItemDao,
                                    permissions, appHash, appCertHash, versionName, versionCode)

                            } else {
                                //Fresh Installation
                                handleFreshInstallation(apkItemDao, apkChangeLogDao, context)

                            }
                        }
                    }

                    // Send to Backend component to check for Apk Legitimacy
                    sendForApkVerification(packageName, context)
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


//    Handle Reinstallation of an existing application in apk_database
//    @params: context: Current Apk Context
//    @params apkRetrieved: Retrieve the apk from installed_apk
//    @params: apkItemDao: ApkItemDao for database operations on installed_apk
//    @params: permissions, appHash, appCertHash, versionName, versionCode:
//              information retrieved from re-installed apk
//    @return: void
    private suspend fun handleReinstallation(
    context: Context,
    apkRetrieved: ApkItem,
    apkItemDao: ApkItemDao,
    permissions: String,
    appHash: String,
    appCertHash: String,
    versionName: String,
    versionCode: Int
    ) {
        val isSamePermissions = permissions?.equals(ListToStringConverterUtil.listToString(apkRetrieved.permissions)) // Convert both permissions to String then compare
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
        println(conditions.joinToString(","))

        apkRetrieved?.let {
            it.isDeleted = false
            it.appName = apkRetrieved.appName
            it.packageName = apkRetrieved.packageName
            it.appIcon = apkRetrieved.appIcon
            this.versionCode = this.versionCode
            this.versionName = this.versionName
            it.installDate = System.currentTimeMillis()
            it.lastUpdateDate = System.currentTimeMillis()
            it.permissions = ListToStringConverterUtil.stringToList(this.permissions)
            it.isSystemApp = apkRetrieved.isSystemApp
            it.appCertHash = this.appCertHash
            it.appHash = this.appHash
            it.timestamp = System.currentTimeMillis()
            apkItemDao.updateApkItem(it)
        }

        if (conditions.all { it!! }) {/*Trigger Backend component
                                    *
                                    *
                                    */
            } else {


            }

        withContext(Dispatchers.Main) {
            NotificationUtil.sendNotification(
                context, "App Reinstallation Detected", "${apkRetrieved.appName}: ${apkRetrieved.packageName} uninstalled at ${DateUtil.formatDate(System.currentTimeMillis())}."
            )
        }
    }

    private suspend fun handlePackageRemoved(
        context: Context, packageName: String?, apkItemDao: ApkItemDao
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apkItem = packageName?.let { apkItemDao.getApkItemByPackageName(it) }

                if (apkItem != null) {
                    // Soft Deletion of apkItem
                    apkItem?.let {
                        it.isDeleted = true
                        apkItemDao.updateApkItem(it)
                        Log.d(
                            "Apk Sentinel",
                            "Apk:${apkItem.packageName} isDeleted has been updated, to ${it.isDeleted}"
                        )
                    }
                    withContext(Dispatchers.Main) {
                        NotificationUtil.sendNotification(
                            context, "App Uninstalled", "${apkItem.appName}: ${apkItem.packageName} uninstalled at ${DateUtil.formatDate(System.currentTimeMillis())}."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("Apk Sentinel", "Error message: ${e.message}")
            }
        }

    }

    private suspend fun handleFreshInstallation(
        apkItemDao: ApkItemDao,
        apkChangeLogDao: ApkChangeLogDao,
        context: Context?,
    ) {
        if (context == null || packageName == null) {
            return
        }
        val changeLogEntity = ApkChangeLogEntity(
            packageName= packageName,
            appHash= appHash,
            oldAppCertHash= appCertHash,
            newAppCertHash="",
            permissionsRemoved=ListToStringConverterUtil.stringToList(""),
            permissionsAdded = ListToStringConverterUtil.stringToList(permissions)
        )
        println(changeLogEntity)
        apkChangeLogDao.insert(changeLogEntity)
        println(apkChangeLogDao.getAll())


        // Insert into installed_apk
        val apkToInsert = ApkItem(
            appName = appName,
            packageName = packageName,
            appIcon = DrawableUtil.convertDrawableToBase64String(appIcon).toString(),
            versionName = versionName,
            versionCode = versionCode,
            installDate = installDate,
            lastUpdateDate = lastUpdateDate,
            permissions = ListToStringConverterUtil.stringToList(permissions),
            isSystemApp = isSystemApp,
            appHash = appHash,
            appCertHash = appCertHash,
            isDeleted = false,
        )
        apkItemDao.insert(apkToInsert)


        // Check if appCertHash exists in installed_apk
        val listOfTrustedAppCertHash: List<String> = apkItemDao.getAllAppCertHash()
        val isTrustedIncomingAppCertHash = listOfTrustedAppCertHash.contains(this.appCertHash)
        val message: String =
            "$packageName's App Cert is " + if (isTrustedIncomingAppCertHash) "trusted" else "not trusted"




        withContext(Dispatchers.Main) {
            NotificationUtil.sendNotification(
                context,
                "New App Installation Detected",
                message
            )
        }
    }
    companion object {
        fun newInstance(): ApkInstallReceiver {
            return ApkInstallReceiver()
        }
    }

}
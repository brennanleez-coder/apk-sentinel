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
        Log.d("Apk Sentinel - ApkInstallReceiver", "$action: $packageName")

        val packageManager = context.packageManager
        val database = ApkItemDatabase.getDatabase(context)
        val apkItemDao = database.apkItemDao()
        val apkChangeLogDao = database.apkChangeLogDao()


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
                            handleUpdate(apkItemDao, apkChangeLogDao, context)
                        }
                        Intent.ACTION_PACKAGE_ADDED -> {
                            /* Listen to app installation
                            *  CASE 1: Fresh installation
                            *  CASE 2: Re-installation
                            */
                            if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {

                                val apkRetrieved = apkItemDao.getApkItemByPackageName(packageName)
                                if (apkRetrieved != null) {
                                    // Reinstallation
                                    handleReinstallation(context, apkRetrieved, apkItemDao, apkChangeLogDao)
                                } else {
                                    // Fresh Installation
                                    handleFreshInstallation(apkItemDao, apkChangeLogDao, context)
                                }
                            }

                        }
                    }
                    // Send to Backend component to check for Apk Legitimacy
                    sendApkForVerification(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("Apk Sentinel - ApkInstallReceiver", "$context: Package Name Not Found")
                } catch (exception: Exception) {
                    Log.e("Apk Sentinel - ApkInstallReceiver", "$exception")
                }
            }
        }
        if (action == Intent.ACTION_PACKAGE_REMOVED) {
            if (packageName != null) {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // Handle package removed (not part of an update)
                    handlePackageRemoved(context, packageName, apkItemDao)
                }

            }
        }

    }

    //    Handle Update of an existing application in apk_database
    //    @params: apkItemDao: ApkItemDao for database operations on installed_apk
    //    @params: apkChangeLogDao: ApkChangeLogDao for database operations on apk_change_log
    //    @params: context: Context of the Application
    //    @return: void
    private suspend fun handleUpdate(
        apkItemDao: ApkItemDao,
        apkChangeLogDao: ApkChangeLogDao,
        context: Context
    ) {
        val apkItem = apkItemDao.getApkItemByPackageName(packageName)
        if (apkItem != null) {
            val (listOfPermissionsAdded, listOfPermissionsRemoved) = checkPermissionChanges(apkItem)
            updateApkEntityInDatabase(apkItem, apkItemDao, true)

            val changeLogEntity = ApkChangeLogEntity(
                packageName = this.packageName,
                versionName = this.versionName,
                versionCode = this.versionCode,
                appHash = this.appHash,
                oldAppCertHash = this.appCertHash,
                newAppCertHash = apkItem.appCertHash,
                permissionsRemoved = listOfPermissionsRemoved,
                permissionsAdded = listOfPermissionsAdded
            )
            apkChangeLogDao.insert(changeLogEntity)

            withContext(Dispatchers.Main) {
                NotificationUtil.sendNotification(
                    context,
                    "App Update",
                    "$appName: $packageName updated to version code: $versionCode at ${DateUtil.formatDate(System.currentTimeMillis())}"
                )
            }
        }
    }

    //    Send Apk Information to Sentinel Sight
    //    @params: context: packageName: Package Name of the captured intent
    //    @return: void
    private suspend fun sendApkForVerification(packageName: String) {
        val apkInfo = ApkInformation(packageName, this.versionName, this.versionCode, this.appHash, this.appCertHash, this.permissions)
        val jsonBody = Gson().toJson(apkInfo)

        try {
            val response = HttpUtil.post("http://10.0.2.2:8000/submit_apk", jsonBody)
            val responseObj = Gson().fromJson(response, ApiResponse::class.java)
            if (responseObj.status == "success") {
                Log.d("Apk Sentinel - Verifying Apk", "$packageName sent to Sentinel Sight")
            } else {
                Log.d(
                        "Apk Sentinel - Verifying Apk Error",
                        "$packageName was NOT sent to Sentinel Sight"
                )
            }
        } catch (e: Exception) {
            Log.e("Apk Sentinel - ApkInstallReceiver", "${e.printStackTrace()}")

        }
    }


    //    Handle Reinstallation of an existing application in apk_database
    //    @params: context: Current Apk Context
    //    @params apkRetrieved: Retrieve the apk from database
    //    @params: apkItemDao: ApkItemDao for database operations on installed_apk
    //    @return: void
    private suspend fun handleReinstallation(
    context: Context,
    apkRetrieved: ApkItem,
    apkItemDao: ApkItemDao,
    apkChangeLogDao: ApkChangeLogDao
    ) {
        val isSamePermissions = this.permissions?.equals(ListToStringConverterUtil.listToString(apkRetrieved.permissions)) // Convert both permissions to String then compare
        val isSameAppHash = this.appHash == apkRetrieved.appHash
        val isSameAppCertHash = this.appCertHash.equals(apkRetrieved.appCertHash)
        val isSameVersionName = this.versionName == apkRetrieved.versionName
        val isSameVersionCode = this.versionCode == apkRetrieved.versionCode

        val conditions = listOf(
            isSamePermissions,
            isSameAppHash,
            isSameAppCertHash,
            isSameVersionName,
            isSameVersionCode
        )
        Log.d("Apk Sentinel - ApkInstallReceiver", "$conditions")

        // Check if appCertHash is different from previous installation
        val message: String =
            "App Cert is " + if (isSameAppCertHash) "trusted" else "not trusted"

        if (!conditions.all { it!! }) {
            if (!isSamePermissions!!) {
                val (listOfPermissionsAdded, listOfPermissionsRemoved) = checkPermissionChanges(apkRetrieved)

                val newChangeLogEntity = ApkChangeLogEntity(
                    packageName= packageName,
                    versionName = this.versionName,
                    versionCode = this.versionCode,
                    appHash= this.appHash,
                    oldAppCertHash= apkRetrieved.appCertHash,
                    newAppCertHash= this.appCertHash,
                    permissionsRemoved=listOfPermissionsRemoved,
                    permissionsAdded = listOfPermissionsAdded
                )
                apkChangeLogDao.insert(newChangeLogEntity)


            }
        }
        updateApkEntityInDatabase(apkRetrieved, apkItemDao)


        withContext(Dispatchers.Main) {
                NotificationUtil.sendNotification(
                    context, "App Reinstallation Detected", "${apkRetrieved.appName}: ${apkRetrieved.packageName}, $message, reinstalled at ${DateUtil.formatDate(System.currentTimeMillis())}."
                )
        }
    }

    //    Determines Permissions Added or Removed during Installation and Update
    //    @params apkRetrieved: Apk retrieved from installed_apk
    //    @return Pair(permissionsAdded, permissionsRemoved): Pair<List<String>, List<String>>:
    private fun checkPermissionChanges(apkRetrieved: ApkItem): Pair<List<String>, List<String>> {
        val oldPermissionsSet = apkRetrieved.permissions.toSet() // Already a List<String>
        val newPermissionsSet = this.permissions.split(",").map { it.trim() }.toSet()
        Log.d("Apk Sentinel - ApkInstallReceiver", "\"Old permission set: $oldPermissionsSet\"")
        Log.d("Apk Sentinel - ApkInstallReceiver", "New permission set: $newPermissionsSet")


        // Permissions added in the new set
        val setOfPermissionsAdded = newPermissionsSet.subtract(oldPermissionsSet)
        Log.d("Apk Sentinel - ApkInstallReceiver", "permissions added: $setOfPermissionsAdded")

        // Permissions removed from the old set
        val setOfPermissionsRemoved = oldPermissionsSet.subtract(newPermissionsSet)
        Log.d("Apk Sentinel - ApkInstallReceiver", "permissions removed: $setOfPermissionsRemoved")

        val listOfPermissionsAdded = setOfPermissionsAdded.toList()
        val listOfPermissionsRemoved = setOfPermissionsRemoved.toList()
        return Pair(listOfPermissionsAdded, listOfPermissionsRemoved)
    }

    //    Handle update of an existing entity in installed_apk
    //    @params: apkRetrieved: Current record of apk retrieved from installed_apk
    //    @params: apkItemDao: ApkItemDao for database operations on installed_apk
    //    @params: triggeredFromUpdate: Whether this method is called due to an update, defaultvalue = false
    //    @return: void
    private fun updateApkEntityInDatabase(
        apkRetrieved: ApkItem,
        apkItemDao: ApkItemDao,
        triggedFromUpdate: Boolean = false,
    ) {

        apkRetrieved?.let {
            it.isDeleted = false
            it.appName = apkRetrieved.appName
            it.packageName = apkRetrieved.packageName
            it.appIcon = apkRetrieved.appIcon
            it.versionCode = this.versionCode // New version code
            it.versionName = this.versionName // New version name
            if (triggedFromUpdate) {
                // Install Date should not be changed upon update
                it.installDate = it.installDate
            } else {
                it.installDate = System.currentTimeMillis()
            }

            it.lastUpdateDate = System.currentTimeMillis()
            it.permissions =
                ListToStringConverterUtil.stringToList(this.permissions) // New permissions
            it.isSystemApp = apkRetrieved.isSystemApp
            it.appCertHash = this.appCertHash // Possibly new app cert hash
            it.appHash = this.appHash // New app hash
            it.timestamp = System.currentTimeMillis()
            apkItemDao.updateApkItem(it)
        }
    }

    //    Handle uninstallation of a package
    //    @params: packageName: Package name of the captured intent
    //    @params: context: Context of the current application
    //    @params: apkItemDao: ApkItemDao for database operations on installed_apk
    //    @return: void
    private suspend fun handlePackageRemoved(
        context: Context, packageName: String?, apkItemDao: ApkItemDao
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apkItem = packageName?.let { apkItemDao.getApkItemByPackageName(it) }

                if (apkItem != null) {
                    // Soft Deletion of apkItem in installed_apk
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

    //    Handles first time installation of a package on the device
    //    @params: apkItemDao: ApkItemDao for database operations on installed_apk
    //    @params: apkChangeLogDao: ApkChangeLogDao for database operations on apk_change_log
    //    @params: context: Context of the current application
    //    @return: void
    private suspend fun handleFreshInstallation(
        apkItemDao: ApkItemDao,
        apkChangeLogDao: ApkChangeLogDao,
        context: Context?,
    ) {
        if (context == null || packageName == null) {
            return
        }
        // Insert into apk_change_log
        val changeLogEntity = ApkChangeLogEntity(
            packageName= packageName,
            versionName = this.versionName,
            versionCode = this.versionCode,
            appHash= appHash,
            oldAppCertHash= "",
            newAppCertHash=appCertHash,
            permissionsRemoved=ListToStringConverterUtil.stringToList(""),
            permissionsAdded = ListToStringConverterUtil.stringToList(this.permissions)
        )
        apkChangeLogDao.insert(changeLogEntity)


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
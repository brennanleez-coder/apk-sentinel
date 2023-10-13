package com.example.apksentinel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.model.ApkItem
import com.example.apksentinel.utils.DrawableUtil
import com.example.apksentinel.utils.HashUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ApkSentinel : Application() {

    lateinit var apkItemDatabase: ApkItemDatabase

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        try {
            val database = ApkItemDatabase.getDatabase(this)
            val apkItemDao = database.apkItemDao()

            coroutineScope.launch(Dispatchers.IO) {
                val apkCount = apkItemDao.getCount()

                // If the database is not populated, get installed packages
                if (apkCount <= 0) {
                    getInstalledPackagesAsync(this@ApkSentinel, apkItemDao).await()
                    Log.d("Apk Sentinel", "Populated the database with installed packages.")
                } else {
                    val allApks = apkItemDao.getAllApkItems()
                    allApks.collect { list ->
                        list.forEach { apkItem ->
                            Log.d("Apk Item", apkItem.appName)
                        }
                    }
                }
            }
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Room cannot verify the data integrity") == true) {
                Log.e("RoomError", "Schema has changed without an update in version number!")
            } else {
                Log.e("Error", e.message ?: "An error occurred")
            }
        }
//        CoroutineScope(Dispatchers.IO).launch {
//            this@ApkSentinel.deleteDatabase("installed_apk")
//        }

//         Check if the database is populated in the background using Dispatchers.IO

    }

    private fun getInstalledPackagesAsync(context: Context, apkItemDao: ApkItemDao) = coroutineScope.async(
        Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS) // Use PackageManager.GET_PERMISSIONS flag to retrieve permissions - `packageManager.getInstalledPackages(0)`: This retrieves basic information about all installed packages, without any additional details like permissions, services, etc.
        val apkList: MutableList<ApkItem> = mutableListOf()
//        val sigs: Array<Signature> = context.packageManager.getPackageInfo(
//            context.packageName,
//            PackageManager.GET_SIGNATURES
//        ).signatures
//        for (sig in sigs) {
//            Log.d("Apk Sentinel", sig.toString())
//        }
        val database = ApkItemDatabase.getDatabase(context)
        Log.d("Apk Sentinel", "Retrieved Database Instance")
        val apkItemDao = database.apkItemDao()
        Log.d("Apk Sentinel", "Retrieved Apk Item Dao")
        for (packageInfo in packages) {
            val packageName = packageInfo.packageName
            val appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
            val appIcon = packageManager.getApplicationIcon(packageName)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
            val installDate = packageInfo.firstInstallTime
            val lastUpdateDate = packageInfo.lastUpdateTime
            val permissions = packageInfo.requestedPermissions
            val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
//            Log.d("Apk Sentinel", permissions.joinToString("\n"))

            val apkPath = packageInfo.applicationInfo.sourceDir
            val hash = HashUtil.getSHA256HashOfFile(apkPath)


            val apkItem = ApkItem(
                appName,
                packageName,
                appIcon,
                versionName,
                versionCode,
                installDate,
                lastUpdateDate,
                permissions,
                isSystemApp,
                hash
            )
            apkList.add(
                apkItem
            )

        }
        apkList
    }
    private fun insertIntoApkDatabase(apkItemDao: ApkItemDao, apkItem: ApkItem) {


        // Create a new ApkItem with the Base64 string instead of the Drawable
        val (
            appName,
            packageName,
            appIcon,
            versionName,
            versionCode,
            installDate,
            lastUpdateDate,
            permissions,
            isSystemApp,
            appHash
        ) = apkItem
        val base64Icon = DrawableUtil.convertDrawableToBase64String(appIcon)

        val apkEntity = permissions?.let {
            com.example.apksentinel.database.entities.ApkItem(
                appName = appName,
                packageName = packageName,
                appIcon = base64Icon.toString(),
                versionName = versionName,
                versionCode = versionCode,
                installDate = installDate,
                lastUpdateDate = lastUpdateDate,
                permissions = it.toList(),
                isSystemApp = isSystemApp,
                appHash = appHash
            )
        }
        if (apkEntity != null) {
            apkItemDao.insert(apkEntity)
        }


    }



}

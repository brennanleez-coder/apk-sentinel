package com.example.apksentinel

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.model.ApkItem
import com.example.apksentinel.receiver.ApkInstallReceiver
import com.example.apksentinel.receiver.DeveloperOptionsReceiver
import com.example.apksentinel.utils.DrawableUtil
import com.example.apksentinel.utils.HashUtil
import com.example.apksentinel.utils.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApkSentinel : Application() {

    private val apkInstallReceiver = ApkInstallReceiver()
    private val developerOptionsReceiver = DeveloperOptionsReceiver()

    private val applicationScope = CoroutineScope(Dispatchers.Main)

    val isInitialized = MutableLiveData(false)


    override fun onCreate() {
        super.onCreate()

        isInitialized.value = false


        NotificationUtil.createNotificationChannel(this)
        setupApkReceiver()
        setupDevOptionsReceiver();

        try {
            val database = ApkItemDatabase.getDatabase(this)
            val apkItemDao = database.apkItemDao()

            applicationScope.launch {
                try {
                    withContext(Dispatchers.IO) {

                        val apkCount = apkItemDao.getCount()
                        var localApkList: MutableList<ApkItem> = getInstalledPackagesAsync(this@ApkSentinel, apkItemDao)

                        // If the database is not populated, get installed packages
                        if (apkCount <= 0) {
                            localApkList.map {
                                insertIntoApkDatabase(apkItemDao, apkItem = it)

                            }
                            isInitialized.postValue(true)
                            NotificationUtil.sendNotification(this@ApkSentinel, "Apk Sentinel", "Database populated")
                        } else {
                            // Sync database with current phone state
                            val databaseApks = apkItemDao.getAllApkItems() //returns a Flow<List<ApkItem>>
                            databaseApks.collect { databaseApkList ->
                                //For each item in localApkList, check if there is not such app in database, this returns new apps
                                // LOGIC ERROR
                                val newApps = localApkList.filter { newItem ->
                                    databaseApkList.none { oldItem -> oldItem.packageName == newItem.packageName }
                                }
                                newApps.forEach {
                                    insertIntoApkDatabase(apkItemDao, apkItem = it)
    //                            Log.d("Apk Sentinel", "New app found: ${it.packageName}")
                                }
                                //for each item in databaseApkList, check if there is no such app in local phone state, this returns deleted apps
                                val deletedApps = databaseApkList.filter { oldItem ->
                                    localApkList.none { newItem -> newItem.packageName == oldItem.packageName}
                                }

                                deletedApps.forEach {
                                    //toggle soft deletion
                                    it.isDeleted = true
                                    apkItemDao.updateApkItem(it)
                                }

    //                        Log.d("Apk Sentinel", list.size.toString() + " retrieved")
                            }

                            isInitialized.postValue(true)
                        }
                        NotificationUtil.sendNotification(this@ApkSentinel, "Apk Sentinel", "Database synced")
                    }
                } catch (e: Exception) {
                    isInitialized.postValue(false)
                    Log.e("ApkSentinelInit", "Exception during database operations", e)
                }
            }
        } catch (e: IllegalStateException) {
            isInitialized.postValue(false)
            if (e.message?.contains("Room cannot verify the data integrity") == true) {
                Log.e("RoomError", "Schema has changed without an update in version number!")
            } else {
                Log.e("Error", e.message ?: "An error occurred")
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Log.e("Apk Sentinel","$e.message")
        } catch (e: RuntimeException) {
            e.printStackTrace()
            Log.e("Apk Sentinel","$e.message")
        }
    }

    private fun setupDevOptionsReceiver() {
        val filter = IntentFilter()
        filter.addAction("DeveloperOptionsChanged")
        filter.addAction("UsbDebuggingChanged")
        registerReceiver(developerOptionsReceiver, filter)
    }
    private fun setupApkReceiver() {
        val filter = IntentFilter()
        filter.addAction("android.intent.action.PACKAGE_REMOVED")
        filter.addAction("android.intent.action.PACKAGE_ADDED")
        filter.addAction("android.intent.action.PACKAGE_REPLACED")
        filter.addDataScheme("package")
        registerReceiver(apkInstallReceiver, filter)
    }

    private fun getInstalledPackagesAsync(context: Context, apkItemDao: ApkItemDao): MutableList<ApkItem> {

        val packageManager = context.packageManager
        val packages =
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES) // Add PackageManager.GET_SIGNATURES flag to retrieve signatures
        val apkList: MutableList<ApkItem> = mutableListOf()

        for (packageInfo in packages) {
            val packageName = packageInfo.packageName
            val appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
            val appIcon = packageManager.getApplicationIcon(packageName)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
            val installDate = packageInfo.firstInstallTime
            val lastUpdateDate = packageInfo.lastUpdateTime
            val permissions = packageInfo.requestedPermissions
            val isSystemApp =
                (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
//            Log.d("Apk Sentinel", permissions.joinToString("\n"))

            val apkPath = packageInfo.applicationInfo.sourceDir
//            Log.d("Check Path", "$packageName: $apkPath")
//            val appHash = HashUtil.getSHA256HashOfFile(apkPath)
            val appHash = HashUtil.hashApk(apkPath, "SHA-256")

            val appCertHash = packageInfo?.signatures?.get(0)?.toCharsString()

            val apkItem = appCertHash?.let {
                ApkItem(
                    appName,
                    packageName,
                    appIcon,
                    versionName,
                    versionCode,
                    installDate,
                    lastUpdateDate,
                    permissions,
                    isSystemApp,
                    appHash,
                    it,
                    false
                )
            }
            if (apkItem != null) {
                apkList.add(apkItem)
            }

            apkList
        }

        return apkList
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
            appHash,
            appCertHash,
            isDeleted
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
                appHash = appHash,
                appCertHash = appCertHash,
                isDeleted = isDeleted,
            )
        }
        //converting from model.ApkItem to database.entity.ApkItem
        if (apkEntity != null) {
            apkItemDao.insert(apkEntity)
        }

    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(apkInstallReceiver)
        unregisterReceiver(developerOptionsReceiver)

    }




}

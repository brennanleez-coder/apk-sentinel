package com.example.apksentinel

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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

class ApkSentinel : Application() {

    val apkInstallReceiver = ApkInstallReceiver()
    val developerOptionsReceiver = DeveloperOptionsReceiver()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> get() = _isInitialized
//    private val handler = Handler(Looper.getMainLooper())
//    private val notificationRunnable: Runnable = object : Runnable {
//        override fun run() {
//            NotificationUtil.sendNotification(this@ApkSentinel, "asd", "asd")
//            handler.postDelayed(this, 5000) // 5 seconds
//        }
//    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannel(this)
//        NotificationUtil.sendNotification(this@ApkSentinel, "Test", "Test")
//        handler.post(notificationRunnable)

        setupApkReceiver()
        setupDevOptionsReceiver();

        try {

            val database = ApkItemDatabase.getDatabase(this)
            val apkItemDao = database.apkItemDao()

            coroutineScope.launch(Dispatchers.IO) {
                val apkCount = apkItemDao.getCount()
                var apkList: MutableList<ApkItem>

                // If the database is not populated, get installed packages
                if (apkCount <= 0) {
                    apkList = getInstalledPackagesAsync(this@ApkSentinel, apkItemDao).await()
                    apkList.map {
                        insertIntoApkDatabase(apkItemDao, apkItem = it)
                        Log.d("Apk Sentinel", it.toString())
                    }
                } else {
                // Sync database with current phone state
                    val allApks = apkItemDao.getAllApkItems()
                    allApks.collect { list ->
                        Log.d("Apk Sentinel", list.size.toString() + " retrieved")
                        NotificationUtil.sendNotification(this@ApkSentinel, "Apk Sentinel", "Dashboard Updated")

                    }
                }
                _isInitialized.postValue(true)

            }
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Room cannot verify the data integrity") == true) {
                Log.e("RoomError", "Schema has changed without an update in version number!")
            } else {
                Log.e("Error", e.message ?: "An error occurred")
            }
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

    private fun getInstalledPackagesAsync(context: Context, apkItemDao: ApkItemDao) = coroutineScope.async(
        Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES) // Add PackageManager.GET_SIGNATURES flag to retrieve signatures
        val apkList: MutableList<ApkItem> = mutableListOf()

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
            val appHash = HashUtil.getSHA256HashOfFile(apkPath)
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

        }
        apkList //important for coroutines
//        Log.d("Apk Sentinel", "Populated the database with installed packages.")

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


//    suspend fun isHashDifferentForNewApk(appName: String, newHash: String): Boolean {
//        val existingHash = apkItemDao.getHashForApp(appName)
//        return existingHash != null && existingHash != newHash
//    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(apkInstallReceiver)
        unregisterReceiver(developerOptionsReceiver)

    }




}

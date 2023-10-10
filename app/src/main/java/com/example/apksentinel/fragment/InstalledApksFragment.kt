package com.example.apksentinel.fragment

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apksentinel.R
import com.example.apksentinel.adapter.ApkListAdapter
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.model.ApkItem
import com.example.apksentinel.utils.DrawableUtil
import com.example.apksentinel.utils.HashUtil.getSHA256HashOfFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class InstalledApksFragment : Fragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private lateinit var recyclerView: RecyclerView
    private lateinit var apkListAdapter: ApkListAdapter
    private lateinit var tvApkCount: TextView
    private lateinit var loaderProgressBar: ProgressBar

    private lateinit var apkItemDao: ApkItemDao

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_installed_apks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvInstalledApks)
        recyclerView.layoutManager = LinearLayoutManager(context)
        apkListAdapter = ApkListAdapter(emptyList())
        recyclerView.adapter = apkListAdapter
        tvApkCount = view.findViewById(R.id.tvApkCount)
        loaderProgressBar = view.findViewById<ProgressBar>(R.id.loaderProgressBar)


        loaderProgressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        apkItemDao = ApkItemDatabase.getDatabase(this.requireContext()).apkItemDao()
        Log.d("Apk Sentinel", apkItemDao.toString() + "Created!")

        coroutineScope.launch {
            val apkList = getInstalledPackagesAsync(requireContext()).await()
            apkListAdapter.updateData(apkList)
            tvApkCount.text = apkList.size.toString()

            // Delay needed to ensure the list is loaded before starting the animation
            recyclerView.postDelayed({
                // Scroll down by a set amount (e.g., 50 pixels) to show scroll animation
                recyclerView.smoothScrollBy(0, 100)

                // After a short delay, scroll back up
                recyclerView.postDelayed({
                    recyclerView.smoothScrollBy(0, -100)
                }, 500) // delay for scrolling back up
            }, 500) // initial delay for scrolling down
            loaderProgressBar.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }





    }

    private fun getInstalledPackagesAsync(context: Context) = coroutineScope.async(Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS) // Use PackageManager.GET_PERMISSIONS flag to retrieve permissions
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


            val apkPath = packageInfo.applicationInfo.sourceDir
            val hash = getSHA256HashOfFile(apkPath)
            apkList.add(
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
                    hash
                )
            )
        }
        apkList
    }

    private fun insertIntoApkDatabase(apkItem: ApkItem) {
        val base64Icon = DrawableUtil.convertDrawableToBase64String(apkItem.appIcon)

//        // Create a new ApkItem with the Base64 string instead of the Drawable
//        val newApkItem = apkItem.copy(appIcon = base64Icon ?: "")
//
//        // Insert the newApkItem into your database
//        val database = ApkItemDatabase.getDatabase(context)
//        val apkItemDao = database.apkItemDao()
//        apkItemDao.insert(newApkItem)

    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    companion object {
        fun newInstance(): InstalledApksFragment {
            return InstalledApksFragment()
        }
    }
}

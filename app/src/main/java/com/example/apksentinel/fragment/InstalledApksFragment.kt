package com.example.apksentinel.fragment

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import android.widget.Spinner
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

    private lateinit var searchView: SearchView
    private lateinit var spinner: Spinner

    private lateinit var apkItemDao: ApkItemDao

    private var allAppsList: List<ApkItem> = listOf()


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
        loaderProgressBar = view.findViewById(R.id.loaderProgressBar)
        searchView = view.findViewById(R.id.searchView)
        spinner = view.findViewById(R.id.filterSpinner)

        loaderProgressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        apkItemDao = ApkItemDatabase.getDatabase(this.requireContext()).apkItemDao()
        Log.d("Apk Sentinel", apkItemDao.toString() + "Created!")

        coroutineScope.launch {
            val apkList = getInstalledPackagesAsync(requireContext()).await()
            allAppsList = apkList

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


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Do something when the user submits a search query
                filterList(query, spinner.selectedItem.toString())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Do something in real-time as the user types
                filterList(newText, spinner.selectedItem.toString())
                return true
            }
        })

        // Set up the spinner (dropdown)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                filterList(searchView.query.toString(), selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }


    }

    private fun getInstalledPackagesAsync(context: Context) = coroutineScope.async(Dispatchers.IO) {
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
//            insertIntoApkDatabase(apkItem)
        }
        apkList
    }


    private fun insertIntoApkDatabase(apkItem: ApkItem) {


        // Create a new ApkItem with the Base64 string instead of the Drawable
        val (
            appName,
            packangeName,
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
                packageName = packangeName,
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


        // Insert the newApkItem into your database
        val database = ApkItemDatabase.getDatabase(this.requireContext())
        val apkItemDao = database.apkItemDao()
        apkItemDao.insert(apkEntity!!)

    }

    private fun filterList(query: String?, filterOption: String) {

        val filteredList = allAppsList.filter {
            it.appName.contains(query ?: "", ignoreCase = true)
        }


        val finalList = when (filterOption) {
            "System Apps" -> filteredList.filter { it.isSystemApp }
            "Non-System Apps" -> filteredList.filter {!it.isSystemApp}
            else -> filteredList // Default to "All Apps"
        }


        apkListAdapter.updateData(finalList)
        tvApkCount.text = finalList.size.toString()
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

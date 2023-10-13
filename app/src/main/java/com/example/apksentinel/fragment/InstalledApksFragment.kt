package com.example.apksentinel.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apksentinel.R
import com.example.apksentinel.adapter.ApkListAdapter
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.dao.ApkItemDao
import com.example.apksentinel.database.entities.ApkItem
import com.example.apksentinel.utils.DateUtil
import com.example.apksentinel.utils.DrawableUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        val database = ApkItemDatabase.getDatabase(this.requireContext())
        Log.d("Apk Sentinel", "Retrieved Database Instance")
        val apkItemDao = database.apkItemDao()
        Log.d("Apk Sentinel", "Retrieved Apk Item Dao")


        coroutineScope.launch {
            try {
                apkItemDao.getAllApkItems().collect { apkList ->
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
            } catch (e: Exception) {
                // Handle the exception here, for instance:
                Log.e("DatabaseError", "Error retrieving items from database: ${e.message}")

                // Hide loader if visible
                loaderProgressBar.visibility = View.GONE

                // Optional: Show a user-friendly message or UI update
                 Toast.makeText(context, "Failed to load data! Try restarting the application", Toast.LENGTH_LONG).show()
            }
        }



//        coroutineScope.launch {
//            val apkList = apkItemDao.getAllApkItems()
//            allAppsList = apkList
//
//            apkListAdapter.updateData(apkList)
//            tvApkCount.text = apkList.size.toString()
//
//            // Delay needed to ensure the list is loaded before starting the animation
//            recyclerView.postDelayed({
//                // Scroll down by a set amount (e.g., 50 pixels) to show scroll animation
//                recyclerView.smoothScrollBy(0, 100)
//
//                // After a short delay, scroll back up
//                recyclerView.postDelayed({
//                    recyclerView.smoothScrollBy(0, -100)
//                }, 500) // delay for scrolling back up
//            }, 500) // initial delay for scrolling down
//            loaderProgressBar.visibility = View.GONE
//            recyclerView.visibility = View.VISIBLE
//        }

        apkListAdapter.listener = object : ApkListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                showDialog(position)
            }
        }



        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query, spinner.selectedItem.toString())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText, spinner.selectedItem.toString())
                return true
            }
        })

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                filterList(searchView.query.toString(), selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }


    }

    private fun filterList(query: String?, filterOption: String) {

        val filteredList = allAppsList.filter {
            it.appName.contains(query ?: "", ignoreCase = true)
        }


        val finalList = when (filterOption) {
            "System Apps" -> filteredList.filter { it.isSystemApp }
            "Non-System Apps" -> filteredList.filter {!it.isSystemApp}
            "Most Permissions" -> filteredList.sortedWith(compareByDescending { it.permissions?.size ?: Int.MIN_VALUE })
            else -> filteredList
        }


        apkListAdapter.updateData(finalList)
        tvApkCount.text = finalList.size.toString()
    }

    private fun showDialog(position: Int) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_layout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Get the APK item at the clicked position
        val apkItem = allAppsList[position]

        // Set data from the apkItem to your dialog's views
        val appName: TextView = dialog.findViewById(R.id.appName)
        val appIcon: ImageView = dialog.findViewById(R.id.appIcon)
        val packageName: TextView = dialog.findViewById(R.id.packageName)
        val versionInfo: TextView = dialog.findViewById(R.id.versionInfo)
        val installDate: TextView = dialog.findViewById(R.id.installDate)
        val lastUpdateDate: TextView = dialog.findViewById(R.id.lastUpdateDate)
        val permissionsHeader: TextView = dialog.findViewById(R.id.permissionsHeader)
        val permissions: TextView = dialog.findViewById(R.id.permissions)
        val isSystemApp: TextView = dialog.findViewById(R.id.isSystemApp)
        val appHash: TextView = dialog.findViewById(R.id.appHash)

        appName.text = apkItem.appName
        val drawableIcon = DrawableUtil.convertBase64StringToDrawable(apkItem.appIcon, this.requireContext())
        appIcon.setImageDrawable(drawableIcon)
        packageName.text = apkItem.packageName
        versionInfo.text = apkItem.versionName
        installDate.text = DateUtil.formatDate(apkItem.installDate)
        lastUpdateDate.text = DateUtil.formatDate(apkItem.lastUpdateDate)


        val numPermissions = apkItem.permissions?.size ?: 0

        permissionsHeader.text = if (numPermissions > 0) {
            "Permissions ($numPermissions)"
        } else {
            "No permissions required"
        }

        if (numPermissions == 0) {
            permissions.visibility = View.GONE
        } else {
            permissions.text = apkItem.permissions!!.joinToString("\n")
        }


        isSystemApp.text = if (apkItem.isSystemApp.toString() == "True") "Yes" else "No"
        appHash.text = apkItem.appHash



        dialog.show()
        val closeButton: ImageView = dialog.findViewById(R.id.closeButton)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
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

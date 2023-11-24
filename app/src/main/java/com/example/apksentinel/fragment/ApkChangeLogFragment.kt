package com.example.apksentinel.fragment

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
import com.example.apksentinel.adapter.ApkChangeLogAdapter
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.entities.ApkChangeLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//import kotlinx.android.synthetic.main.fragment_apk_change_log.*

class ApkChangeLogFragment : Fragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var recyclerView: RecyclerView
    private lateinit var changeLogAdapter: ApkChangeLogAdapter
    private lateinit var loaderProgressBar: ProgressBar
    private lateinit var tvChangeLogCount: TextView
    private lateinit var tvEmptyState: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_apk_change_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.apkChangeLogRecyclerView)
        loaderProgressBar = view.findViewById(R.id.loaderProgressBar)
        tvChangeLogCount = view.findViewById(R.id.tvChangeLogCount)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)


        recyclerView.layoutManager = LinearLayoutManager(context)
        val mockLogs = listOf(
            ApkChangeLogEntity(
                packageName = "com.example.mockapp1",
                appHash = "appHash1",
                oldAppCertHash = "oldHash1",
                newAppCertHash = "newHash1",
                permissionsAdded = listOf("CAMERA", "MICROPHONE"),
                permissionsRemoved = listOf(),
                timestamp = System.currentTimeMillis()
            ),
            ApkChangeLogEntity(
                packageName = "com.example.mockapp2",
                appHash = "appHash2",
                oldAppCertHash = "oldHash2",
                newAppCertHash = "newHash2",
                permissionsAdded = listOf(),
                permissionsRemoved = listOf("LOCATION"),
                timestamp = System.currentTimeMillis()
            )
            // Add more mock logs as required
        )
        changeLogAdapter = ApkChangeLogAdapter(emptyList())
        recyclerView.adapter = changeLogAdapter

        loaderProgressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val database = ApkItemDatabase.getDatabase(this.requireContext())
        val changeLogDao = database.apkChangeLogDao()

        coroutineScope.launch {
            try {
                val logs = changeLogDao.getAll()

                withContext(Dispatchers.Main) {
                    if (logs.isEmpty()) {
                        // If real logs are empty, update the adapter with mock data
                        changeLogAdapter.updateData(mockLogs)
                        tvChangeLogCount.text = "Total Logs: ${mockLogs.size}"
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        loaderProgressBar.visibility = View.GONE
                    } else {
                        // If real logs are present, show the real logs
                        changeLogAdapter.updateData(logs)
                        tvChangeLogCount.text = "Total Logs: ${logs.size}"
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        loaderProgressBar.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("DatabaseError", "Error retrieving change logs from database: ${e.message}")
                    loaderProgressBar.visibility = View.GONE
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
//        coroutineScope.cancel()
    }


    companion object {
        fun newInstance(): ApkChangeLogFragment {
            return ApkChangeLogFragment()
        }
    }
}

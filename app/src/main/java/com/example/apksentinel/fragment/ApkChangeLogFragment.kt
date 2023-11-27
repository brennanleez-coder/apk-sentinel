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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
                        // If real logs are present, show the real logs
                    changeLogAdapter.updateData(logs.sortedByDescending { it.timestamp })
                    tvChangeLogCount.text = "Total Logs: ${logs.size}"
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    loaderProgressBar.visibility = View.GONE
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
    }


    companion object {
        fun newInstance(): ApkChangeLogFragment {
            return ApkChangeLogFragment()
        }
    }
}

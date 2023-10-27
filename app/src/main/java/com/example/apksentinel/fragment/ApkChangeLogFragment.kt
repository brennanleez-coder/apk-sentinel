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
import com.example.apksentinel.database.ApkItemDatabase
import com.example.apksentinel.database.entities.ApkChangeLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//import kotlinx.android.synthetic.main.fragment_apk_change_log.*

class ApkChangeLogFragment : Fragment() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

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

//        loaderProgressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val database = ApkItemDatabase.getDatabase(this.requireContext())
        val changeLogDao = database.apkChangeLogDao()

        coroutineScope.launch {
            try {
                val logs = changeLogDao.getAll()
                if (logs.isEmpty()) {
                    // Show the empty state message and hide the RecyclerView
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    loaderProgressBar.visibility = View.GONE
                } else {
                    changeLogAdapter.updateData(logs)
                    tvChangeLogCount.text = "Total Logs: ${logs.size}"
                    loaderProgressBar.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("DatabaseError", "Error retrieving change logs from database: ${e.message}")
                loaderProgressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        coroutineScope.cancel()
    }

    private class ApkChangeLogAdapter(private var logs: List<ApkChangeLogEntity>) : RecyclerView.Adapter<ApkChangeLogAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val packageNameTextView: TextView = itemView.findViewById(R.id.packageNameTextView)
            val oldAppCertHashTextView: TextView = itemView.findViewById(R.id.oldAppCertHashTextView)
            val newAppCertHashTextView: TextView = itemView.findViewById(R.id.newAppCertHashTextView)
            val permissionsAddedTextView: TextView = itemView.findViewById(R.id.permissionsAddedTextView)
            val permissionsRemovedTextView: TextView = itemView.findViewById(R.id.permissionsRemovedTextView)
            val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        }

        fun updateData(newLogs: List<ApkChangeLogEntity>) {
            logs = newLogs
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.apk_change_log_item_layout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val log = logs[position]

            holder.packageNameTextView.text = log.packageName
            holder.oldAppCertHashTextView.text = "Old Cert Hash: ${log.oldAppCertHash ?: "N/A"}"
            holder.newAppCertHashTextView.text = "New Cert Hash: ${log.newAppCertHash ?: "N/A"}"

            if (log.permissionsAdded != null && log.permissionsAdded.isNotEmpty()) {
                holder.permissionsAddedTextView.text = "Permissions Added: ${log.permissionsAdded.joinToString(", ")}"
            } else {
                holder.permissionsAddedTextView.text = "No Permissions Added"
            }

            if (log.permissionsRemoved != null && log.permissionsRemoved.isNotEmpty()) {
                holder.permissionsRemovedTextView.text = "Permissions Removed: ${log.permissionsRemoved.joinToString(", ")}"
            } else {
                holder.permissionsRemovedTextView.text = "No Permissions Removed"
            }

            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(
                Date(log.timestamp)
            )
            holder.timestampTextView.text = formattedDate
        }

        override fun getItemCount() = logs.size
    }


    companion object {
        fun newInstance(): ApkChangeLogFragment {
            return ApkChangeLogFragment()
        }
    }
}

package com.example.apksentinel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apksentinel.R
import com.example.apksentinel.database.entities.ApkChangeLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApkChangeLogAdapter(var logs: List<ApkChangeLogEntity>) : RecyclerView.Adapter<ApkChangeLogAdapter.ApkChangeLogViewHolder>() {

    class ApkChangeLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val packageNameTextView: TextView = itemView.findViewById(R.id.packageNameTextView)
        val versionNameTextView: TextView = itemView.findViewById(R.id.versionNameTextView)
        val versionCodeTextView: TextView = itemView.findViewById(R.id.versionCodeTextView)
        val appHashTextView: TextView = itemView.findViewById(R.id.appHashTextView)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApkChangeLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.apk_change_log_item_layout, parent, false)
        return ApkChangeLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApkChangeLogViewHolder, position: Int) {
        val log = logs[position]

        holder.packageNameTextView.text = log.packageName
        holder.versionNameTextView.text = "Version Name: ${log.versionName}"
        holder.versionCodeTextView.text = "Version Code: ${log.versionCode.toString()}"
        holder.appHashTextView.text = "App Hash: ${log.appHash}"
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
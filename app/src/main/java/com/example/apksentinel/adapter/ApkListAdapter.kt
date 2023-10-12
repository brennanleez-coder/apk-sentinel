package com.example.apksentinel.adapter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apksentinel.R
import com.example.apksentinel.model.ApkItem

class ApkListAdapter(initialApkList: List<ApkItem>) : RecyclerView.Adapter<ApkListAdapter.ApkItemViewHolder>() {

    private var apkList: MutableList<ApkItem> = initialApkList.toMutableList()

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
    var listener: OnItemClickListener? = null

    class ApkItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        val packageNameTextView: TextView = itemView.findViewById(R.id.packageNameTextView)
        val entryNumberTextView: TextView = itemView.findViewById(R.id.entryNumberTextView)
        val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        val versionNameTextView: TextView = itemView.findViewById(R.id.versionNameTextView)
        val versionCodeTextView: TextView = itemView.findViewById(R.id.versionCodeTextView)


    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApkItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.apk_item_layout, parent, false)
        return ApkItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ApkItemViewHolder, position: Int) {
        val currentItem = apkList[position]
        holder.entryNumberTextView.text = (position + 1).toString() // +1 because position is 0-based
        holder.appNameTextView.text = currentItem.appName
        holder.packageNameTextView.text = currentItem.packageName
        holder.versionNameTextView.text = "Version Name: ${currentItem.versionName}"
        holder.versionCodeTextView.text = "Version Code: ${currentItem.versionCode}"
        try {
            val appIconDrawable = holder.itemView.context.packageManager.getApplicationIcon(currentItem.packageName)
            holder.appIconImageView.setImageDrawable(appIconDrawable)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        holder.itemView.setOnClickListener {
            listener?.onItemClick(position)
        }

    }


    fun updateData(newData: List<ApkItem>) {

        apkList.clear()
        apkList.addAll(newData)
        notifyDataSetChanged()
    }


    override fun getItemCount() = apkList.size
}

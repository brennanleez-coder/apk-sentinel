package com.example.apksentinel.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.apksentinel.fragment.ApkChangeLogFragment
import com.example.apksentinel.fragment.DashboardFragment
import com.example.apksentinel.fragment.DeveloperOptionsFragment
import com.example.apksentinel.fragment.InstalledApksFragment

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()
            1 -> DeveloperOptionsFragment()
            2 -> ApkChangeLogFragment()
            3 -> InstalledApksFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

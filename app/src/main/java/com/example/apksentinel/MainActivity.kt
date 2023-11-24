package com.example.apksentinel

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.apksentinel.adapter.ViewPagerAdapter
import com.example.apksentinel.utils.HttpUtil.post
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            val viewPager: ViewPager2 = findViewById(R.id.viewPager)
            val tabLayout: TabLayout = findViewById(R.id.tabLayout)

            val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
            viewPager.adapter = adapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                when (position) {
                    0 -> tab.text = "Dashboard"
                    1 -> tab.text = "Dev Options"
                    2 -> tab.text = "Change Log"
                    3 -> tab.text = "Apk Info"
                }
            }.attach()
        } catch (e: Exception) {
            Log.e("Apk Sentinel", "Caught in MainActivity $e.printStackTrace().toString() ")
        }
    }


    private fun loadPages (savedInstanceState: Bundle?, vararg pairs: Pair<Int, Fragment>) {
        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            for (pair in pairs) {
                transaction.replace(pair.first, pair.second)
            }
            transaction.commit()
        }
    }
}

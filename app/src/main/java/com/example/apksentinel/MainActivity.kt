package com.example.apksentinel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.apksentinel.adapter.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //load developer options card and installed APK
//        loadPages(savedInstanceState,
//            Pair(R.id.includedDeveloperOptionsCard, DeveloperOptionsFragment.newInstance()),
//            Pair(R.id.containerInstalledApks, InstalledApksFragment.newInstance())
//            )

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)

        // Set up the adapter for ViewPager
        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Dashboard"
                1 -> tab.text = "Developer Options"
                2 -> tab.text = "Installed APK"
            }
        }.attach()


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

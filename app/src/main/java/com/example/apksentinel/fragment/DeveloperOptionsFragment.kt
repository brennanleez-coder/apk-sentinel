package com.example.apksentinel.fragment

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.apksentinel.R

class DeveloperOptionsFragment : Fragment() {

    private lateinit var swDeveloperSettings: Switch
    private lateinit var swUSBDebugging: Switch
    private lateinit var tvNetworkStatus: TextView
    private val connectivityManager by lazy {
        context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus("Network Available")
        }

        override fun onLost(network: Network) {
            updateNetworkStatus("Network Lost")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_developer_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swDeveloperSettings = view.findViewById(R.id.swDeveloperSettings)
        swUSBDebugging = view.findViewById(R.id.swUSBDebugging)
        tvNetworkStatus = view.findViewById(R.id.tvNetworkStatus)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        swDeveloperSettings.isChecked = isDeveloperOptionsEnabled
        swUSBDebugging.isChecked = isUSBDebuggingEnabled
        swUSBDebugging.isEnabled = isDeveloperOptionsEnabled
    }

    override fun onPause() {
        super.onPause()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private val isDeveloperOptionsEnabled: Boolean
        get() {
            return Settings.Global.getInt(
                context?.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
        }

    private val isUSBDebuggingEnabled: Boolean
        get() {
            return Settings.Global.getInt(
                context?.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
        }

    private fun updateNetworkStatus(status: String) {
        tvNetworkStatus.text = "Network Status: $status"
    }

    private fun setupListeners() {
        swDeveloperSettings.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isDeveloperOptionsEnabled) {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)

                if (isChecked) {
                    showToast("Please enable Developer Options!")
                } else {
                    showToast("Please disable Developer Options!")
                }
            }
            swUSBDebugging.isEnabled = isChecked
        }

        swUSBDebugging.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isUSBDebuggingEnabled) {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)

                if (isChecked) {
                    showToast("Please enable USB Debugging!")
                } else {
                    showToast("Please disable USB Debugging!")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(): DeveloperOptionsFragment {
            return DeveloperOptionsFragment()
        }
    }
}

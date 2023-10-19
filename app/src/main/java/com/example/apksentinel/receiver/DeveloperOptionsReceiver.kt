package com.example.apksentinel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.apksentinel.utils.NotificationUtil

class DeveloperOptionsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "DeveloperOptionsChanged" -> handleDeveloperOptionsChange(context)
            "UsbDebuggingChanged" -> handleUsbDebuggingChange(context)
        }
    }

    private fun handleDeveloperOptionsChange(context: Context?) {
        val developerOptionsEnabled =
            Settings.Global.getInt(context?.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

        if (developerOptionsEnabled) {
            val message = "Developer Options has been enabled"
            NotificationUtil.sendNotification(context!!, "Potential Malicious Activity", message)
        }
    }

    private fun handleUsbDebuggingChange(context: Context?) {
        val usbDebuggingOptionsEnabled =
            Settings.Global.getInt(
                context?.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
        if (usbDebuggingOptionsEnabled) {
            val message = "Usb Debugging Mode has been enabled"
            NotificationUtil.sendNotification(context!!, "Potential Malicious Activity", message)
        }
    }
    companion object {
        fun newInstance(): DeveloperOptionsReceiver {
            return DeveloperOptionsReceiver()
        }
    }
}

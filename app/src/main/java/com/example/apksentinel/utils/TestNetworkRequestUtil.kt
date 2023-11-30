package com.example.apksentinel.utils

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TestNetworkRequestUtil {

    private fun testNetworkRequest(serverString: String = "http://10.0.2.2:8000/",
                                   jsonBody: String = """{"key": "value"}"""
    ) {
        val postBody = Gson().toJson(jsonBody)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpUtil.post(serverString, postBody)
                Log.d("Apk Sentinel - TestNetworkRequestUtil", "POST Response: $response")

                val response2 = HttpUtil.get("http://10.0.2.2:8000/")
                Log.d("Apk Sentinel - TestNetworkRequestUtil", "GET Response: $response2")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
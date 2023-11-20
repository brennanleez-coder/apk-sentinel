package com.example.apksentinel.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {

    fun get(url: String): String {
        val urlObj = URL(url)
        val connection = urlObj.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(connection.inputStream)).use {
                val response = StringBuilder()
                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                return response.toString()
            }
        } else {
            throw RuntimeException("HttpGet request failed with response code: $responseCode")
        }
    }
    fun post(url: String, body: String): String {
        val urlObj = URL(url)
        (urlObj.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json") // Set this as needed

            OutputStreamWriter(outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            return if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream.bufferedReader().use { it.readText() }
            } else {
                throw RuntimeException("HttpPost request failed with response code: $responseCode")
            }
        }
    }
}

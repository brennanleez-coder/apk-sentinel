package com.example.apksentinel.utils

import java.io.File
import java.security.MessageDigest

object HashUtil {

    fun getSHA256HashOfFile(filePath: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val fileBytes = filePath.toByteArray()
        val bytes = digest.digest(fileBytes)
        val stringBuilder = StringBuilder()
        for (byte in bytes) {
            stringBuilder.append(String.format("%02X", byte))
        }
        return stringBuilder.toString()
    }

    fun hashApk(apkFilePath: String, algorithm: String): String {
        val file = File(apkFilePath)
        if (!file.exists()) {
            throw IllegalArgumentException("APK file does not exist.")
        }

        try {
            val digest = MessageDigest.getInstance(algorithm)
            val apkBytes = file.readBytes()
            val hashBytes = digest.digest(apkBytes)
            val hashStringBuilder = StringBuilder()

            for (byte in hashBytes) {
                // Convert each byte to a hexadecimal string
                hashStringBuilder.append(String.format("%02x", byte))
            }

            return hashStringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Error hashing APK: ${e.message}")
        }
    }

}

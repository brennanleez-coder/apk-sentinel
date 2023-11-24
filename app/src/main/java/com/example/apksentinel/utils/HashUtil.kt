package com.example.apksentinel.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtil {


    fun hashApkWithSHA256(apkFilePath: String): String {
        val file = File(apkFilePath)
        if (!file.exists()) {
            throw IllegalArgumentException("APK file does not exist.")
        }

        try {
            val digest = MessageDigest.getInstance("SHA-256")

            // Use a buffered input stream to read the file in chunks
            val inputStream = BufferedInputStream(FileInputStream(file))
            val buffer = ByteArray(8 * 1024)  // 8 KB buffer
            var bytesRead: Int

            // Read the file in chunks and update the digest
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }

            // Close the input stream after reading the file
            inputStream.close()

            val hashBytes = digest.digest()
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

package com.example.apksentinel.utils

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
}

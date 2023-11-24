package com.example.apksentinel.utils

import android.content.pm.PackageManager
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object HashUtil {

    fun hashCertWithSHA256(packageName: String, packageManager: PackageManager): String? {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = packageInfo.signatures
            val cert = signatures[0]

            val inputStream = cert.toByteArray().inputStream()
            val certificateFactory = CertificateFactory.getInstance("X509")
            val x509Certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate

            // Compute SHA-256 digest
            val sha256Digest = MessageDigest.getInstance("SHA-256")
            val sha256 = sha256Digest.digest(x509Certificate.encoded).joinToString("") { "%02x".format(it) }

            // Print other digests for debugging (optional)
            val sha1Digest = MessageDigest.getInstance("SHA-1")
            val md5Digest = MessageDigest.getInstance("MD5")
            val sha1 = sha1Digest.digest(x509Certificate.encoded).joinToString("") { "%02x".format(it) }
            val md5 = md5Digest.digest(x509Certificate.encoded).joinToString("") { "%02x".format(it) }
            println("SHA-256: $sha256")
            println("SHA-1: $sha1")
            println("MD5: $md5")

            sha256
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
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

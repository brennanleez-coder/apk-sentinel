package com.example.apksentinel.model

data class ApkInformation(
    var package_name: String,
    var version_name: String,
    var version_code: Int,
    var incoming_apk_hash: String,
    var incoming_app_cert_hash: String,
    var incoming_permissions: String
)

package com.example.apksentinel.utils

/* This file should be used for List to String conversion, vice versa
*  ONLY WHEN it is not in the context of database conversion
*/

object ListToStringConverterUtil {

    fun stringToList(permissions: String) : List<String> {
        return permissions.split(",").filter { it.isNotEmpty() }
    }

    fun listToString(permissions: List<String>) : String {
        return permissions.joinToString(",")
    }
}
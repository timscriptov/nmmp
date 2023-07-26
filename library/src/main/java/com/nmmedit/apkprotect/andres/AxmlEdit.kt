package com.nmmedit.apkprotect.andres

import com.mcal.apkparser.xml.ReadManifest

object AxmlEdit {
    @JvmStatic
    fun getApplicationName(manifestBytes: ByteArray?): String {
        val applicationName = ReadManifest(manifestBytes!!).applicationName
        return if (applicationName.isNullOrEmpty()) {
            ""
        } else {
            applicationName
        }
    }

    @JvmStatic
    fun getMinSdk(manifestBytes: ByteArray?): Int {
        val applicationName = ReadManifest(manifestBytes!!).minSdkVersion
        return if (applicationName.isNullOrEmpty()) {
            21
        } else {
            applicationName.toInt()
        }
    }

    @JvmStatic
    fun getPackageName(manifestBytes: ByteArray?): String {
        val applicationName = ReadManifest(manifestBytes!!).packageName
        return if (applicationName.isNullOrEmpty()) {
            ""
        } else {
            applicationName
        }
    }
}
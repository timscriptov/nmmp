package com.nmmedit.apkprotect.andres

import com.mcal.apkparser.xml.ManifestParser

object AxmlEdit {
    @JvmStatic
    fun getApplicationName(manifestBytes: ByteArray): String {
        val applicationName = ManifestParser(manifestBytes).applicationName
        return if (applicationName.isNullOrEmpty()) {
            ""
        } else {
            applicationName
        }
    }

    @JvmStatic
    fun getMinSdk(manifestBytes: ByteArray): Int {
        val applicationName = ManifestParser(manifestBytes).minSdkVersion
        return if (applicationName.isNullOrEmpty()) {
            21
        } else {
            applicationName.toInt()
        }
    }

    @JvmStatic
    fun getPackageName(manifestBytes: ByteArray): String {
        val applicationName = ManifestParser(manifestBytes).packageName
        return if (applicationName.isNullOrEmpty()) {
            ""
        } else {
            applicationName
        }
    }
}

package com.nmmedit.apkprotect.util

import java.util.Locale

object OsDetector {
    @JvmStatic
    val isWindows: Boolean
        get() {
            val osName = System.getProperty("os.name") ?: return false
            return osName.lowercase(Locale.getDefault()).contains("windows")
        }
}

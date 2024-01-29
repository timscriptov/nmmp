package com.nmmedit.apkprotect.data

import com.nmmedit.apkprotect.util.FileHelper
import java.io.File

object Storage {
    @JvmStatic
    val workingDir: File
        get() {
            return File(File(FileHelper::class.java.protectionDomain.codeSource.location.toURI().path).parentFile.path)
        }

    @JvmStatic
    val binDir: File
        get() {
            val dir = File(workingDir, "bin")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }
}
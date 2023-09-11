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

    @JvmStatic
    val outRootDir: File
        get() {
            val dir = File(workingDir, "output")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    @JvmStatic
    val zipExtractTempDir: File
        get() {
            val dir = File(outRootDir, ".apk_temp")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    @JvmStatic
    val dex2cSrcDir: File
        get() {
            val dir = File(outRootDir, "dex2c")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    @JvmStatic
    val codeGeneratedDir: File
        get() {
            val dir = File(dex2cSrcDir, "generated")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    @JvmStatic
    val tempDexDir: File
        get() {
            val dir = File(outRootDir, "dex_output")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }
}
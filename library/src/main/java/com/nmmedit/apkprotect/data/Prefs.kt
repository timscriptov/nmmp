package com.nmmedit.apkprotect.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nmmedit.apkprotect.data.Storage.binDir
import com.nmmedit.apkprotect.data.config.Config
import com.nmmedit.apkprotect.util.FileHelper
import com.nmmedit.apkprotect.util.FileHelper.readFile
import com.nmmedit.apkprotect.util.OsDetector
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

object Prefs {
    private val configFileName = if (OsDetector.isWindows) {
        "config-windows.json"
    } else {
        "config.json"
    }

    private val configFile = File(binDir, configFileName)

    @JvmStatic
    fun config(): Config {
        if (!configFile.exists() || configFile.length() <= 0) {
            FileHelper.writeToFile(
                configFile,
                Prefs::class.java.getResourceAsStream("/$configFileName") as InputStream
            )
        }
        return try {
            val content = readFile(configFile, StandardCharsets.UTF_8)
            val config = GsonBuilder().create().fromJson(content, Config::class.java)
            config
        } catch (e: Exception) {
            throw RuntimeException("Load config failed $configFile", e)
        }
    }

    @JvmStatic
    val isArm: Boolean
        get() = config().abi?.arm ?: true

    @JvmStatic
    val isArm64: Boolean
        get() = config().abi?.arm64 ?: false

    @JvmStatic
    val isX86: Boolean
        get() = config().abi?.x86 ?: true

    @JvmStatic
    val isX64: Boolean
        get() = config().abi?.x64 ?: false

    @JvmStatic
    fun sdkPath(): String {
        return config().environment?.sdkPath ?: "C:/Android/Sdk"
    }

    @JvmStatic
    fun setSdkPath(path: String) {
        val config = config()
        config.environment?.sdkPath = path
        FileHelper.writeToFile(configFile, Gson().toJson(config))
    }

    @JvmStatic
    fun cmakePath(): String {
        return config().environment?.cmakePath ?: "C:/Android/Sdk/cmake/3.18.1"
    }

    @JvmStatic
    fun setCmakePath(path: String) {
        val config = config()
        config.environment?.cmakePath = path
        FileHelper.writeToFile(configFile, Gson().toJson(config))
    }

    @JvmStatic
    fun ndkPath(): String {
        return config().environment?.ndkPath ?: "C:/Android/Sdk/ndk/25.1.8937393"
    }

    @JvmStatic
    fun setNdkPath(path: String) {
        val config = config()
        config.environment?.ndkPath = path
        FileHelper.writeToFile(configFile, Gson().toJson(config))
    }

    @JvmStatic
    fun ndkToolchains(): String {
        return config().environment?.ndkToolchains ?: "/toolchains/llvm/prebuilt/"
    }

    @JvmStatic
    fun ndkAbi(): String {
        return config().environment?.ndkAbi ?: "windows-x86_64"
    }

    @JvmStatic
    fun ndkStrip(): String {
        return config().environment?.ndkStrip ?: "/bin/llvm-strip"
    }
}

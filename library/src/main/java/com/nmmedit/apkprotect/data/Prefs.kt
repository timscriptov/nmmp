package com.nmmedit.apkprotect.data

import com.nmmedit.apkprotect.util.OsDetector

object Prefs {
    private val config = ConfigManager.instance
    private val stripPath = if (OsDetector.isWindows()) {
        "/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-strip"
    } else {
        "/toolchains/llvm/prebuilt/linux-x86_64//bin/llvm-strip"
    }

    @JvmStatic
    var isArm: Boolean
        get() = config.getBoolean("abi.armeabi-v7a", true)
        set(value) {
            config.edit().putBoolean("abi.armeabi-v7a", value).apply()
        }

    @JvmStatic
    var isArm64: Boolean
        get() = config.getBoolean("abi.arm64-v8a", true)
        set(value) {
            config.edit().putBoolean("abi.arm64-v8a", value).apply()
        }

    @JvmStatic
    var isX86: Boolean
        get() = config.getBoolean("abi.x86", true)
        set(value) {
            config.edit().putBoolean("abi.x86", value).apply()
        }

    @JvmStatic
    var isX64: Boolean
        get() = config.getBoolean("abi.x86_64", true)
        set(value) {
            config.edit().putBoolean("abi.x86_64", value).apply()
        }

    @JvmStatic
    var rulesPath: String
        get() = config.getString("app.rules_path")
        set(value) {
            config.edit().putString("app.rules_path", value).apply()
        }

    @JvmStatic
    var mappingPath: String
        get() = config.getString("app.mapping_path")
        set(value) {
            config.edit().putString("app.mapping_path", value).apply()
        }

    @JvmStatic
    var sdkPath: String
        get() = config.getString("environment.sdk_path", System.getenv("ANDROID_SDK_HOME") ?: "")
        set(value) {
            config.edit().putString("environment.sdk_path", value).apply()
        }

    @JvmStatic
    var cmakePath: String
        get() = config.getString("environment.cmake_path", System.getenv("CMAKE_PATH") ?: "")
        set(value) {
            config.edit().putString("environment.cmake_path", value).apply()
        }

    @JvmStatic
    var ndkPath: String
        get() = config.getString("environment.ndk_path", System.getenv("ANDROID_NDK_HOME") ?: "")
        set(value) {
            config.edit().putString("environment.ndk_path", value).apply()
        }

    @JvmStatic
    var ndkStripBinary: String
        get() = config.getString("environment.ndk_strip_binary_path", ndkPath.ifEmpty { "NDK_PATH" } + stripPath)
        set(value) {
            config.edit().putString("environment.ndk_strip_binary_path", value).apply()
        }

    @JvmStatic
    var cxxFlags: String
        get() = config.getString("cmake.cxx_flags", "-fvisibility=hidden")
        set(value) {
            config.edit().putString("cmake.cxx_flags", value).apply()
        }
}
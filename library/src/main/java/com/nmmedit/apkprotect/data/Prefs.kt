package com.nmmedit.apkprotect.data

import com.mcal.preferences.Preferences
import com.nmmedit.apkprotect.util.OsDetector

object Prefs {
    @JvmStatic
    var arm: Boolean = true
        get() = Preferences.getBoolean("arm", true)
        set(value) {
            Preferences.putBoolean("arm", value)
            field = value
        }

    @JvmStatic
    var arm64: Boolean = true
        get() = Preferences.getBoolean("arm64", true)
        set(value) {
            Preferences.putBoolean("arm64", value)
            field = value
        }

    @JvmStatic
    var x86: Boolean = true
        get() = Preferences.getBoolean("x86", true)
        set(value) {
            Preferences.putBoolean("x86", value)
            field = value
        }

    @JvmStatic
    var x64: Boolean = true
        get() = Preferences.getBoolean("x64", true)
        set(value) {
            Preferences.putBoolean("x64", value)
            field = value
        }

    @JvmStatic
    fun sdkPath(): String {
        return Preferences.getString("sdk_path", System.getenv("ANDROID_SDK_HOME"))
    }

    @JvmStatic
    fun setSdkPath(path: String) {
        Preferences.putString("sdk_path", path)
    }

    @JvmStatic
    fun cmakePath(): String {
        return Preferences.getString("cmake_path", System.getenv("CMAKE_PATH"))
    }

    @JvmStatic
    fun setCmakePath(path: String) {
        Preferences.putString("cmake_path", path)
    }

    @JvmStatic
    fun ndkPath(): String {
        return Preferences.getString("ndk_path", System.getenv("ANDROID_NDK_HOME"))
    }

    @JvmStatic
    fun setNdkPath(path: String) {
        Preferences.putString("ndk_path", path)
    }

    @JvmStatic
    fun ndkToolchains(): String {
        return Preferences.getString("toolchains", "/toolchains/llvm/prebuilt/")
    }

    @JvmStatic
    fun ndkAbi(): String {
        return Preferences.getString(
            "abi",
            if (OsDetector.isWindows) {
                "windows-x86_64"
            } else {
                "linux-x86_64"
            }
        )
    }

    @JvmStatic
    fun ndkStrip(): String {
        return Preferences.getString("strip", "/bin/llvm-strip")
    }
}

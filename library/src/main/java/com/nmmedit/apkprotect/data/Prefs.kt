package com.nmmedit.apkprotect.data

import com.mcal.preferences.Preferences
import com.nmmedit.apkprotect.util.OsDetector

object Prefs {
    @JvmStatic
    fun isArm(): Boolean {
        return Preferences.getBoolean("arm", true)
    }

    @JvmStatic
    fun setArm(mode: Boolean) {
        Preferences.putBoolean("arm", mode)
    }

    @JvmStatic
    fun isArm64(): Boolean {
        return Preferences.getBoolean("arm64", true)
    }

    @JvmStatic
    fun setArm64(mode: Boolean) {
        Preferences.putBoolean("arm64", mode)
    }

    @JvmStatic
    fun isX86(): Boolean {
        return Preferences.getBoolean("x86", true)
    }

    @JvmStatic
    fun setX86(mode: Boolean) {
        Preferences.putBoolean("x86", mode)
    }

    @JvmStatic
    fun isX64(): Boolean {
        return Preferences.getBoolean("x64", true)
    }

    @JvmStatic
    fun setX64(mode: Boolean) {
        Preferences.putBoolean("x64", mode)
    }

    @JvmStatic
    fun getVmName(): String {
        return Preferences.getString("vm_name", "nmmvm")
    }

    @JvmStatic
    fun setVmName(path: String) {
        Preferences.putString("vm_name", path)
    }

    @JvmStatic
    fun getNmmpName(): String {
        return Preferences.getString("nmmp_name", "nmmp")
    }

    @JvmStatic
    fun setNmmpName(path: String) {
        Preferences.putString("nmmp_name", path)
    }

    @JvmStatic
    fun getRegisterNativesClassName(): String {
        return Preferences.getString(
            "register_natives_class_name",
            "com/nmmedit/protect/NativeUtil"
        )
    }

    @JvmStatic
    fun setRegisterNativesClassName(path: String) {
        Preferences.putString("register_natives_class_name", path)
    }

    @JvmStatic
    fun sdkPath(): String {
        return Preferences.getString("sdk_path", System.getenv("ANDROID_SDK_HOME") ?: "")
    }

    @JvmStatic
    fun setSdkPath(path: String) {
        Preferences.putString("sdk_path", path)
    }

    @JvmStatic
    fun getCmakePath(): String {
        return Preferences.getString("cmake_path", System.getenv("CMAKE_PATH") ?: "")
    }

    @JvmStatic
    fun setCmakePath(path: String) {
        Preferences.putString("cmake_path", path)
    }

    @JvmStatic
    fun getNdkPath(): String {
        return Preferences.getString("ndk_path", System.getenv("ANDROID_NDK_HOME") ?: "")
    }

    @JvmStatic
    fun setNdkPath(path: String) {
        Preferences.putString("ndk_path", path)
    }

    @JvmStatic
    fun getNdkToolchains(): String {
        return Preferences.getString("toolchains", "/toolchains/llvm/prebuilt/")
    }

    @JvmStatic
    fun getNdkAbi(): String {
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
    fun getNdkStrip(): String {
        return Preferences.getString("strip", "/bin/llvm-strip")
    }
}

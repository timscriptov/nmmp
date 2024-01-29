package com.nmmedit.apkprotect.data

import com.mcal.preferences.PreferencesManager
import com.nmmedit.apkprotect.data.Storage.binDir
import com.nmmedit.apkprotect.util.OsDetector

object Prefs : PreferencesManager(binDir, "nmmp_preferences.json") {
    @JvmStatic
    fun isArm(): Boolean {
        return getBoolean("arm", true)
    }

    @JvmStatic
    fun setArm(mode: Boolean) {
        putBoolean("arm", mode)
    }

    @JvmStatic
    fun isArm64(): Boolean {
        return getBoolean("arm64", true)
    }

    @JvmStatic
    fun setArm64(mode: Boolean) {
        putBoolean("arm64", mode)
    }

    @JvmStatic
    fun isX86(): Boolean {
        return getBoolean("x86", true)
    }

    @JvmStatic
    fun setX86(mode: Boolean) {
        putBoolean("x86", mode)
    }

    @JvmStatic
    fun isX64(): Boolean {
        return getBoolean("x64", true)
    }

    @JvmStatic
    fun setX64(mode: Boolean) {
        putBoolean("x64", mode)
    }

    @JvmStatic
    fun getVmName(): String {
        return getString("vm_name", "nmmvm")
    }

    @JvmStatic
    fun setVmName(name: String) {
        putString("vm_name", name)
    }

    @JvmStatic
    fun getNmmpName(): String {
        return getString("nmmp_name", "nmmp")
    }

    @JvmStatic
    fun setNmmpName(name: String) {
        putString("nmmp_name", name)
    }

    @JvmStatic
    fun setCxxFlags(flags: String) {
        putString("cxx_flags", flags)
    }

    @JvmStatic
    fun getCxxFlags(): String {
        return getString("cxx_flags", "")
    }

    @JvmStatic
    fun getRegisterNativesClassName(): String {
        return getString(
            "register_natives_class_name",
            "com/nmmedit/protect/NativeUtil"
        )
    }

    @JvmStatic
    fun setRegisterNativesClassName(path: String) {
        putString("register_natives_class_name", path)
    }

    @JvmStatic
    fun getSdkPath(): String {
        return getString("sdk_path", System.getenv("ANDROID_SDK_HOME") ?: "")
    }

    @JvmStatic
    fun setSdkPath(path: String) {
        putString("sdk_path", path)
    }

    @JvmStatic
    fun getCmakePath(): String {
        return getString("cmake_path", System.getenv("CMAKE_PATH") ?: "")
    }

    @JvmStatic
    fun setCmakePath(path: String) {
        putString("cmake_path", path)
    }

    @JvmStatic
    fun getNdkPath(): String {
        return getString("ndk_path", System.getenv("ANDROID_NDK_HOME") ?: "")
    }

    @JvmStatic
    fun setNdkPath(path: String) {
        putString("ndk_path", path)
    }

    @JvmStatic
    fun getNdkToolchains(): String {
        return getString("toolchains", "/toolchains/llvm/prebuilt/")
    }

    @JvmStatic
    fun getNdkAbi(): String {
        return getString(
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
        return getString("strip", "/bin/llvm-strip")
    }
}

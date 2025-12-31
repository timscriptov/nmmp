package com.nmmedit.apkprotect.data

object Prefs {
    private val config = ConfigManager.instance

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
        get() = config.getString("environment.sdk_path")
        set(value) {
            config.edit().putString("environment.sdk_path", value).apply()
        }

    @JvmStatic
    var cmakePath: String
        get() = config.getString("environment.cmake_path")
        set(value) {
            config.edit().putString("environment.cmake_path", value).apply()
        }

    @JvmStatic
    var ndkPath: String
        get() = config.getString("environment.ndk_path")
        set(value) {
            config.edit().putString("environment.ndk_path", value).apply()
        }

    @JvmStatic
    var ndkToolchains: String
        get() = config.getString("environment.ndk_toolchains")
        set(value) {
            config.edit().putString("environment.ndk_toolchains", value).apply()
        }

    @JvmStatic
    var ndkAbi: String
        get() = config.getString("environment.ndk_abi")
        set(value) {
            config.edit().putString("environment.ndk_abi", value).apply()
        }

    @JvmStatic
    var ndkStrip: String
        get() = config.getString("environment.ndk_strip")
        set(value) {
            config.edit().putString("environment.ndk_strip", value).apply()
        }
}
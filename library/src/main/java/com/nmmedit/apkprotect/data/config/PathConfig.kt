package com.nmmedit.apkprotect.data.config

import com.google.gson.annotations.SerializedName

class PathConfig {
    @SerializedName("sdk_path")
    var sdkPath: String = ""

    @SerializedName("cmake_path")
    var cmakePath: String = ""

    @SerializedName("ndk_path")
    var ndkPath: String = ""

    @SerializedName("ndk_toolchains")
    var ndkToolchains: String = ""

    @SerializedName("ndk_abi")
    var ndkAbi: String = ""

    @SerializedName("ndk_strip")
    var ndkStrip: String = ""
}

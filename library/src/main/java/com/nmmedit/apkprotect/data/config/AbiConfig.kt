package com.nmmedit.apkprotect.data.config

import com.google.gson.annotations.SerializedName

class AbiConfig {
    @SerializedName("armeabi-v7a")
    var arm = false

    @SerializedName("arm64-v8a")
    var arm64 = false

    @SerializedName("x86")
    var x86 = false

    @SerializedName("x86_64")
    var x64 = false
}

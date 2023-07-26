package com.nmmedit.apkprotect.data.config

import com.google.gson.annotations.SerializedName

class Config(
    @field:SerializedName("abi")
    var abi: AbiConfig? = null,
    @field:SerializedName("environment")
    var environment: PathConfig? = null
) 
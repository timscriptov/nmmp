package com.nmmedit.apkprotect.log

interface VmpLogger {
    fun info(msg: String?)
    fun error(msg: String?)
    fun warning(msg: String?)
}

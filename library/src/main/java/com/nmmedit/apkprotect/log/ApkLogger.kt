package com.nmmedit.apkprotect.log

interface ApkLogger {
    fun info(msg: String?)
    fun error(msg: String?)
    fun warning(msg: String?)
}

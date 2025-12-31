package com.mcal.apkparser.util

object FileHelper {
    @JvmStatic
    fun readInt(data: ByteArray, off: Int): Int {
        return data[off + 3].toInt() shl 24 or (data[off + 2].toInt() and 0xFF shl 16) or (data[off + 1].toInt() and 0xFF shl 8) or (data[off].toInt() and 0xFF)
    }

    @JvmStatic
    fun writeInt(data: ByteArray, off: Int, value: Int) {
        var i = off
        data[i++] = (value and 0xFF).toByte()
        data[i++] = (value ushr 8 and 0xFF).toByte()
        data[i++] = (value ushr 16 and 0xFF).toByte()
        data[i] = (value ushr 24 and 0xFF).toByte()
    }
}

package com.nmmedit.apkprotect.util

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object FileHelper {
    @JvmStatic
    fun readFile(file: File, encoding: Charset): String {
        return file.inputStream().readBytes().toString(encoding)
    }

    @JvmStatic
    fun readFile(path: String, encoding: Charset): String {
        return File(path).inputStream().readBytes().toString(encoding)
    }

    @JvmStatic
    fun writeToFile(file: File, content: String) {
        file.writeBytes(content.toByteArray(StandardCharsets.UTF_8))
    }

    @JvmStatic
    fun writeToFile(file: File, inputStream: InputStream) {
        file.writeBytes(inputStream.readBytes())
    }

    @JvmStatic
    fun writeToFile(path: String, content: String) {
        File(path).writeBytes(content.toByteArray(StandardCharsets.UTF_8))
    }

    @JvmStatic
    fun writeToFile(path: String, inputStream: InputStream) {
        File(path).writeBytes(inputStream.readBytes())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyStream(inputStream: InputStream, out: OutputStream) {
        val buf = ByteArray(4 * 1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } != -1) {
            out.write(buf, 0, len)
        }
    }

    @JvmStatic
    fun deleteFile(file: File) {
        file.deleteRecursively()
    }
}

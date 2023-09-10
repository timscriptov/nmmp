package com.nmmedit.apkprotect.util

import com.mcal.apkparser.zip.ZipFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object FileHelper {
    /**
     * Возвращает содержимое файла с указанным именем из Zip-архива.
     *
     * @param zipFile Zip-архив, из которого нужно прочитать файл.
     * @param filename Имя файла, который нужно прочитать.
     * @return Массив байтов, содержащий содержимое файла.
     * @throws IOException Если произошла ошибка при чтении файла из Zip-архива.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getZipFileContent(zipFile: File, filename: String): ByteArray {
        ZipFile(zipFile).use {
            it.getInputStream(it.getEntry(filename)).use { inputStream ->
                return inputStream.readBytes()
            }
        }
    }

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

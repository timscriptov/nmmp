package com.nmmedit.apkprotect.util

import com.mcal.apkparser.zip.ZipFile
import java.io.*
import java.util.*
import java.util.regex.Pattern

object ZipHelper {
    /**
     * Returns true if there exists a file whose name matches `filename` in `apkFile`.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun hasFile(apkFile: File, filename: String): Boolean {
        ZipFile(apkFile).use { apkZip -> return apkZip.getEntry(filename) != null }
    }

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

    /**
     * Returns all files in an apk that match a given regular expression.
     *
     * @param apkFile The file containing the apk zip archive.
     * @param regex   A regular expression to match the requested filenames.
     * @return A mapping of the matched filenames to their byte contents.
     * @throws IOException Thrown if a matching file cannot be read from the apk.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getFiles(apkFile: File, regex: String): Map<String, ByteArray> {
        return getFiles(apkFile, Pattern.compile(regex))
    }

    /**
     * Returns all files in an apk that match a given regular expression.
     *
     * @param apkFile The file containing the apk zip archive.
     * @param regex   A regular expression to match the requested filenames.
     * @return A mapping of the matched filenames to their byte contents.
     * @throws IOException Thrown if a matching file cannot be read from the apk.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getFiles(apkFile: File, regex: Pattern): Map<String, ByteArray> {
        ZipFile(apkFile).use { apkZip ->
            val result = LinkedHashMap<String, ByteArray>()
            val entries = apkZip.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (regex.matcher(entry.name).matches()) {
                    apkZip.getInputStream(entry).use { inputStream ->
                        result.put(
                            entry.name,
                            inputStream.readBytes()
                        )
                    }
                }
            }
            return result
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun extractFiles(apkFile: File, regex: String, outDir: File): List<File> {
        return extractFiles(apkFile, Pattern.compile(regex), outDir)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun extractFiles(apkFile: File, regex: Pattern, outDir: File): List<File> {
        ZipFile(apkFile).use { apkZip ->
            val result = LinkedList<File>()
            val entries = apkZip.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && regex.matcher(entry.name).matches()) {
                    val file = File(outDir, entry.name)
                    if (!file.parentFile.exists()) {
                        file.parentFile.mkdirs()
                    }
                    apkZip.getInputStream(entry).use { inputStream ->
                        FileOutputStream(file).use { output ->
                            copyStream(inputStream, output)
                            result.add(file)
                        }
                    }
                }
            }
            return result
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyStream(inputStream: InputStream, out: OutputStream) {
        val buf = ByteArray(8 * 1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } != -1) {
            out.write(buf, 0, len)
        }
    }
}
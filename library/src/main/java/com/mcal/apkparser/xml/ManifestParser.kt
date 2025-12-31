package com.mcal.apkparser.xml

import com.mcal.apkparser.util.FileHelper
import java.io.*

class ManifestParser : Closeable {
    private var byteArray: ByteArray? = null
    private var decoder: AXmlDecoder? = null
    private var parser: AXmlResourceParser? = null

    constructor(path: String) : this(File(path).readBytes())
    constructor(file: File) : this(file.readBytes())
    constructor(inputStream: InputStream) : this(inputStream.readBytes())
    constructor(byteArray: ByteArray) {
        reload(byteArray)
    }

    @Throws(IOException::class)
    private fun reload(byteArray: ByteArray) {
        this.byteArray = byteArray.also { bytes ->
            bytes.inputStream().use { inputStream ->
                decoder = AXmlDecoder.decode(inputStream).also {
                    parser = AXmlResourceParser().apply {
                        open(ByteArrayInputStream(it.data), it.mTableStrings)
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun close() {
        decoder = null
        parser = null
        byteArray = null
    }

    private fun findAttributeStringValue(name: String, attributeNameResource: Int): String? {
        try {
            val parser = parser
            if (parser != null) {
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (parser.name == name) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeNameResource(i) == attributeNameResource) {
                                return parser.getAttributeValue(i)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun findAttributeIntValue(name: String, attributeNameResource: Int): Int {
        try {
            val parser = parser
            if (parser != null) {
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (parser.name == name) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeNameResource(i) == attributeNameResource) {
                                return parser.getAttributeIntValue(i, -1)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }

    private fun findAttributeStringValue(name: String, attributeName: String): String? {
        try {
            val parser = parser
            if (parser != null) {
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (parser.name == name) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i) == attributeName) {
                                return parser.getAttributeValue(i)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun findAttributeListValue(name: String, attributeNameResource: Int): List<String> {
        val list = arrayListOf<String>()
        try {
            val parser = parser
            if (parser != null) {
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (parser.name == name) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeNameResource(i) == attributeNameResource) {
                                list.add(parser.getAttributeValue(i))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun findAttributeBooleanValue(name: String, attributeNameResource: Int): Boolean {
        try {
            val parser = parser
            if (parser != null) {
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) {
                        continue
                    }
                    if (parser.name == name) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeNameResource(i) == attributeNameResource) {
                                return parser.getAttributeBooleanValue(i, false)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @Throws(IOException::class)
    private fun patching(attributeValue: String, name: String, attributeNameResource: Int) {
        val decoder = decoder
        if (decoder != null) {
            val parser = parser
            if (parser != null) {
                var success = false
                var type: Int
                var isFoundAttribute = false
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type == XmlPullParser.START_TAG && parser.getName() == name) {
                        val size = parser.getAttributeCount()
                        for (i in 0 until size) {
                            if (parser.getAttributeNameResource(i) == attributeNameResource) {
                                isFoundAttribute = true
                                val index = decoder.mTableStrings.size
                                val data = decoder.data
                                var off = parser.currentAttributeStart + 20 * i
                                off += 8
                                FileHelper.writeInt(data, off, index)
                                off += 8
                                FileHelper.writeInt(data, off, index)
                            }
                        }
                        if (!isFoundAttribute) {
                            var off = parser.currentAttributeStart
                            val data = decoder.data
                            val newData = ByteArray(data.size + 20)
                            System.arraycopy(data, 0, newData, 0, off)
                            System.arraycopy(data, off, newData, off + 20, data.size - off)

                            // chunkSize
                            val chunkSize: Int = FileHelper.readInt(newData, off - 32)
                            FileHelper.writeInt(newData, off - 32, chunkSize + 20)
                            // attributeCount
                            FileHelper.writeInt(newData, off - 8, size + 1)
                            val idIndex = parser.findResourceID(attributeNameResource)
                            if (idIndex == -1) throw IOException("idIndex == -1")
                            var isMax = true
                            for (i in 0 until size) {
                                val id = parser.getAttributeNameResource(i)
                                if (id > attributeNameResource) {
                                    isMax = false
                                    if (i != 0) {
                                        System.arraycopy(newData, off + 20, newData, off, 20 * i)
                                        off += 20 * i
                                    }
                                    break
                                }
                            }
                            if (isMax) {
                                System.arraycopy(newData, off + 20, newData, off, 20 * size)
                                off += 20 * size
                            }
                            FileHelper.writeInt(
                                newData,
                                off,
                                decoder.mTableStrings.find(SCHEMAS)
                            )
                            FileHelper.writeInt(newData, off + 4, idIndex)
                            FileHelper.writeInt(newData, off + 8, decoder.mTableStrings.size)
                            FileHelper.writeInt(newData, off + 12, TYPE_STRING)
                            FileHelper.writeInt(newData, off + 16, decoder.mTableStrings.size)
                            decoder.data = newData
                        }
                        success = true
                        break
                    }
                }
                if (!success) throw IOException()
                val list = ArrayList<String>(decoder.mTableStrings.size)
                decoder.mTableStrings.getStrings(list)
                list.add(attributeValue)
                ByteArrayOutputStream().use {
                    decoder.write(list, it)
                    reload(it.toByteArray())
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun removeAttribute(name: String, attributeNameResource: Int) {
        val decoder = decoder
        if (decoder != null) {
            val parser = parser
            if (parser != null) {
                var success = false
                while (true) {
                    val type = parser.next()
                    if (type == XmlPullParser.END_DOCUMENT) {
                        break
                    } else if (type == XmlPullParser.START_TAG && parser.getName() == name) {
                        val size = parser.getAttributeCount()
                        var i = 0
                        while (true) {
                            if (i >= size) {
                                break
                            } else if (parser.getAttributeNameResource(i) != attributeNameResource) {
                                i++
                            } else {
                                val attrStart = parser.currentAttributeStart
                                val off = attrStart + 20 * i
                                val data = decoder.getData()
                                val newData = ByteArray(data.size - 20)
                                System.arraycopy(data, 0, newData, 0, off)
                                System.arraycopy(data, off + 20, newData, off, data.size - off - 20)
                                val chunkSize = FileHelper.readInt(newData, attrStart - 32)
                                FileHelper.writeInt(newData, attrStart - 32, chunkSize - 20)
                                FileHelper.writeInt(newData, attrStart - 8, size - 1)
                                decoder.setData(newData)
                                break
                            }
                        }
                        success = true
                    }
                }
                if (!success) {
                    throw IOException("Failed to modify AndroidManifest.xml")
                }
                val list = ArrayList<String>(decoder.mTableStrings.size)
                decoder.mTableStrings.getStrings(list)
                ByteArrayOutputStream().use {
                    decoder.write(list, it)
                    reload(it.toByteArray())
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun patching(attributeValue: String, name: String, attributeName: String) {
        val decoder = decoder
        if (decoder != null) {
            val parser = parser
            if (parser != null) {
                var success = false
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) continue
                    if (parser.name == name) {
                        var isFoundAttribute = false
                        val size = parser.attributeCount
                        for (i in 0 until size) {
                            if (parser.getAttributeName(i) == attributeName) {
                                isFoundAttribute = true
                                val index = decoder.mTableStrings.size
                                val data = decoder.data
                                var off = parser.currentAttributeStart + 20 * i
                                off += 8
                                FileHelper.writeInt(data, off, index)
                                off += 8
                                FileHelper.writeInt(data, off, index)
                            }
                        }
                        if (!isFoundAttribute) {
                            var off = parser.currentAttributeStart
                            val data = decoder.data
                            val newData = ByteArray(data.size + 20)
                            System.arraycopy(data, 0, newData, 0, off)
                            System.arraycopy(data, off, newData, off + 20, data.size - off)

                            // chunkSize
                            val chunkSize = FileHelper.readInt(newData, off - 32)
                            FileHelper.writeInt(newData, off - 32, chunkSize + 20)

                            // attributeCount
                            FileHelper.writeInt(newData, off - 8, size + 1)
                            val idIndex = parser.findResourceID(NAME)
                            if (idIndex == -1) {
                                throw IOException("idIndex == -1")
                            }
                            var isMax = true
                            for (i in 0 until size) {
                                val id = parser.getAttributeNameResource(i)
                                if (id > NAME) {
                                    isMax = false
                                    if (i != 0) {
                                        System.arraycopy(
                                            newData,
                                            off + 20,
                                            newData,
                                            off,
                                            20 * i
                                        )
                                        off += 20 * i
                                    }
                                    break
                                }
                            }
                            if (isMax) {
                                System.arraycopy(newData, off + 20, newData, off, 20 * size)
                                off += 20 * size
                            }
                            FileHelper.writeInt(
                                newData,
                                off,
                                decoder.mTableStrings.find(SCHEMAS)
                            )
                            FileHelper.writeInt(newData, off + 4, idIndex)
                            FileHelper.writeInt(newData, off + 8, decoder.mTableStrings.size)
                            FileHelper.writeInt(newData, off + 12, TYPE_STRING)
                            FileHelper.writeInt(newData, off + 16, decoder.mTableStrings.size)
                            decoder.data = newData
                        }
                        success = true
                        break
                    }
                }
                if (!success) {
                    throw IOException()
                }
                val list = ArrayList<String>(decoder.mTableStrings.size)
                decoder.mTableStrings.getStrings(list)
                list.add(attributeValue)
                ByteArrayOutputStream().use {
                    decoder.write(list, it)
                    reload(it.toByteArray())
                }
            }
        }
    }

    // Public API methods
    fun getPackageName(): String? = findAttributeStringValue("manifest", "package")

    @Throws(IOException::class)
    fun setPackageName(attributeValue: String) = patching(attributeValue, "manifest", "package")

    fun getLabel(): String? = findAttributeStringValue("application", LABEL)

    fun getIcon(): String? = findAttributeStringValue("application", ICON)

    fun getTheme(): String? = findAttributeStringValue("application", THEME)

    fun getApplicationName(): String? = findAttributeStringValue("application", NAME)

    @Throws(IOException::class)
    fun setApplicationName(attributeValue: String) = patching(attributeValue, "application", NAME)

    fun getAppComponentFactoryName(): String? = findAttributeStringValue("application", APP_COMPONENT_FACTORY)

    @Throws(IOException::class)
    fun setAppComponentFactoryName(attributeValue: String) = patching(attributeValue, "application", APP_COMPONENT_FACTORY)

    fun getVersionCode(): String? = findAttributeStringValue("manifest", VERSION_CODE)

    @Throws(IOException::class)
    fun setVersionCode(attributeValue: String) = patching(attributeValue, "manifest", VERSION_CODE)

    fun getVersionName(): String? = findAttributeStringValue("manifest", VERSION_NAME)

    @Throws(IOException::class)
    fun setVersionName(attributeValue: String) = patching(attributeValue, "manifest", VERSION_NAME)

    fun getCompileSdkVersion(): String? = findAttributeStringValue("manifest", COMPILE_SDK_VERSION)

    @Throws(IOException::class)
    fun setCompileSdkVersion(attributeValue: String) = patching(attributeValue, "manifest", COMPILE_SDK_VERSION)

    fun getCompileSdkVersionCodename(): String? = findAttributeStringValue("manifest", COMPILE_SDK_VERSION_CODENAME)

    @Throws(IOException::class)
    fun setCompileSdkVersionCodename(attributeValue: String) = patching(attributeValue, "manifest", COMPILE_SDK_VERSION_CODENAME)

    fun getMinSdkVersion(): Int = findAttributeIntValue("manifest", MIN_SDK_VERSION)

    @Throws(IOException::class)
    fun setMinSdkVersion(attributeValue: String) = patching(attributeValue, "manifest", MIN_SDK_VERSION)

    fun getTargetSdkVersion(): String? = findAttributeStringValue("manifest", TARGET_SDK_VERSION)

    @Throws(IOException::class)
    fun setTargetSdkVersion(attributeValue: String) = patching(attributeValue, "manifest", TARGET_SDK_VERSION)

    fun getExtractNativeLibs(): Boolean = findAttributeBooleanValue("application", EXTRACT_NATIVE_LIBS)

    @Throws(IOException::class)
    fun setExtractNativeLibs(enabled: Boolean) = patching(enabled.toString(), "application", EXTRACT_NATIVE_LIBS)

    fun getAllowBackup(): Boolean = findAttributeBooleanValue("application", ALLOW_BACKUP)

    @Throws(IOException::class)
    fun setAllowBackup(enabled: Boolean) = patching(enabled.toString(), "application", ALLOW_BACKUP)

    fun getLargeHeap(): Boolean = findAttributeBooleanValue("application", LARGE_HEAP)

    @Throws(IOException::class)
    fun setLargeHeap(enabled: Boolean) = patching(enabled.toString(), "application", LARGE_HEAP)

    fun getSupportsRtl(): Boolean = findAttributeBooleanValue("application", SUPPORTS_RTL)

    @Throws(IOException::class)
    fun setSupportsRtl(enabled: Boolean) = patching(enabled.toString(), "application", SUPPORTS_RTL)

    fun getUsesCleartextTraffic(): Boolean = findAttributeBooleanValue("application", USES_CLEARTEXT_TRAFFIC)

    @Throws(IOException::class)
    fun setUsesCleartextTraffic(enabled: Boolean) = patching(enabled.toString(), "application", USES_CLEARTEXT_TRAFFIC)

    fun getRequestLegacyExternalStorage(): Boolean = findAttributeBooleanValue("application", REQUEST_LEGACY_EXTERNAL_STORAGE)

    @Throws(IOException::class)
    fun setRequestLegacyExternalStorage(enabled: Boolean) = patching(enabled.toString(), "application", REQUEST_LEGACY_EXTERNAL_STORAGE)

    fun getPreserveLegacyExternalStorage(): Boolean = findAttributeBooleanValue("application", PRESERVE_LEGACY_EXTERNAL_STORAGE)

    @Throws(IOException::class)
    fun setPreserveLegacyExternalStorage(enabled: Boolean) = patching(enabled.toString(), "application", PRESERVE_LEGACY_EXTERNAL_STORAGE)

    fun getAllServiceName(): List<String> = findAttributeListValue("service", NAME)

    fun getAllReceiverName(): List<String> = findAttributeListValue("receiver", NAME)

    fun getAllActivityName(): List<String> = findAttributeListValue("activity", NAME)

    // Constants
    companion object {
        /**
         * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/res/res/values/public-final.xml
         */
        const val EXTRACT_NATIVE_LIBS = 0x010104ea

        const val APP_COMPONENT_FACTORY = 0x0101057a
        const val VERSION_CODE = 0x0101021b
        const val VERSION_NAME = 0x0101021c

        const val THEME = 0x01010000
        const val LABEL = 0x01010001
        const val ICON = 0x01010002
        const val NAME = 0x01010003

        const val COMPILE_SDK_VERSION = 0x01010572
        const val COMPILE_SDK_VERSION_CODENAME = 0x01010573
        const val ALLOW_BACKUP = 0x01010280
        const val LARGE_HEAP = 0x0101035a
        const val SUPPORTS_RTL = 0x010103af
        const val USES_CLEARTEXT_TRAFFIC = 0x010104ec
        const val REQUEST_LEGACY_EXTERNAL_STORAGE = 0x01010603
        const val PRESERVE_LEGACY_EXTERNAL_STORAGE = 0x01010614
        const val MIN_SDK_VERSION = 0x0101020c
        const val TARGET_SDK_VERSION = 0x01010270

        const val TYPE_STRING = 0x03000008

        const val SCHEMAS = "http://schemas.android.com/apk/res/android"
    }
}
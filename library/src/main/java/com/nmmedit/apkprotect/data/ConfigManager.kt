package com.nmmedit.apkprotect.data

import com.nmmedit.apkprotect.util.FileUtils
import com.nmmedit.apkprotect.util.OsDetector
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ConfigManager private constructor() {
    companion object {
        private const val CONFIG_FILENAME_LINUX = "config.txt"
        private const val CONFIG_FILENAME_WINDOWS = "config-windows.txt"

        val instance: ConfigManager by lazy { ConfigManager() }
    }

    private val configFileName: String
        get() = if (OsDetector.isWindows()) CONFIG_FILENAME_WINDOWS else CONFIG_FILENAME_LINUX

    private val configFilePath: String
        get() = File(FileUtils.getHomePath(), "tools/${configFileName}").absolutePath

    @Volatile
    private var configCache: Map<String, String>? = null

    private val editor: Editor by lazy { Editor() }

    fun string(key: String, defaultValue: String = ""): ReadWriteProperty<Any?, String> =
        ConfigProperty(key, defaultValue)

    fun boolean(key: String, defaultValue: Boolean = false): ReadWriteProperty<Any?, Boolean> =
        ConfigProperty(key, defaultValue)

    fun int(key: String, defaultValue: Int = 0): ReadWriteProperty<Any?, Int> =
        ConfigProperty(key, defaultValue)

    fun long(key: String, defaultValue: Long = 0L): ReadWriteProperty<Any?, Long> =
        ConfigProperty(key, defaultValue)

    fun float(key: String, defaultValue: Float = 0f): ReadWriteProperty<Any?, Float> =
        ConfigProperty(key, defaultValue)

    fun stringSet(key: String, defaultValue: Set<String> = emptySet()): ReadWriteProperty<Any?, Set<String>> =
        ConfigProperty(key, defaultValue)

    fun getString(key: String, defaultValue: String = ""): String =
        getConfigValue(key, defaultValue)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        getConfigValue(key, defaultValue.toString()).toBooleanStrictOrNull() ?: defaultValue

    fun getInt(key: String, defaultValue: Int = 0): Int =
        getConfigValue(key, defaultValue.toString()).toIntOrNull() ?: defaultValue

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        getConfigValue(key, defaultValue.toString()).toLongOrNull() ?: defaultValue

    fun getFloat(key: String, defaultValue: Float = 0f): Float =
        getConfigValue(key, defaultValue.toString()).toFloatOrNull() ?: defaultValue

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> =
        getConfigValue(key, "").takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: defaultValue

    fun contains(key: String): Boolean = key in loadConfig()

    fun getAll(): Map<String, *> = loadConfig()

    fun edit(): Editor = editor

    inner class Editor {
        private val edits = ConcurrentHashMap<String, Any?>()

        fun putString(key: String, value: String?): Editor {
            edits[key] = value
            return this
        }

        fun putBoolean(key: String, value: Boolean): Editor {
            edits[key] = value
            return this
        }

        fun putInt(key: String, value: Int): Editor {
            edits[key] = value
            return this
        }

        fun putLong(key: String, value: Long): Editor {
            edits[key] = value
            return this
        }

        fun putFloat(key: String, value: Float): Editor {
            edits[key] = value
            return this
        }

        fun putStringSet(key: String, value: Set<String>?): Editor {
            edits[key] = value
            return this
        }

        fun remove(key: String): Editor {
            edits[key] = null
            return this
        }

        fun clear(): Editor {
            edits.clear()
            return this
        }

        fun commit(): Boolean {
            return try {
                synchronized(this@ConfigManager) {
                    val configFile = File(configFilePath)
                    ensureConfigFileExists(configFile)

                    // Загружаем текущую конфигурацию
                    val config = loadConfig().toMutableMap()

                    // Применяем изменения
                    edits.forEach { (key, value) ->
                        when (value) {
                            null -> config.remove(key)
                            is Set<*> -> config[key] = (value as Set<String>).joinToString(",")
                            else -> config[key] = value.toString()
                        }
                    }

                    // Сохраняем в файл
                    saveConfigToFile(configFile, config)

                    // Обновляем кэш
                    configCache = config

                    // Очищаем правки
                    edits.clear()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun apply() {
            commit()
        }
    }

    // Внутренние методы
    private fun loadConfig(): Map<String, String> {
        return configCache ?: synchronized(this) {
            configCache ?: loadConfigInternal().also { configCache = it }
        }
    }

    private fun loadConfigInternal(): Map<String, String> {
        val configFile = File(configFilePath)
        ensureConfigFileExists(configFile)
        return parseConfigFile(configFile)
    }

    private fun ensureConfigFileExists(configFile: File) {
        if (configFile.exists()) return

        configFile.parentFile?.mkdirs()
        try {
            Prefs::class.java.getResourceAsStream("/$configFileName")?.use { inputStream ->
                FileOutputStream(configFile).use { outputStream ->
                    FileUtils.copyStream(inputStream, outputStream)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun parseConfigFile(configFile: File): Map<String, String> {
        val config = mutableMapOf<String, String>()
        try {
            configFile.forEachLine { line ->
                val trimmedLine = line.trim()
                when {
                    trimmedLine.isEmpty() -> return@forEachLine
                    trimmedLine.startsWith("#") -> return@forEachLine
                    trimmedLine.startsWith("//") -> return@forEachLine
                    else -> {
                        val equalsIndex = trimmedLine.indexOf('=')
                        if (equalsIndex > 0) {
                            val key = trimmedLine.substring(0, equalsIndex).trim()
                            val value = trimmedLine.substring(equalsIndex + 1).trim()
                            config[key] = value
                        }
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Load config failed", e)
        }
        return config
    }

    private fun getConfigValue(key: String, defaultValue: String = ""): String {
        val config = loadConfig()
        return config[key] ?: defaultValue
    }

    private fun saveConfigToFile(configFile: File, config: Map<String, String>) {
        try {
            configFile.bufferedWriter().use { writer ->
                val linesToPreserve = mutableListOf<String>()
                val existingKeys = mutableSetOf<String>()

                // Читаем и обрабатываем существующий файл
                if (configFile.exists()) {
                    configFile.forEachLine { line ->
                        val trimmedLine = line.trim()
                        when {
                            trimmedLine.isEmpty() -> {
                                linesToPreserve.add(line)
                            }

                            trimmedLine.startsWith("#") || trimmedLine.startsWith("//") -> {
                                linesToPreserve.add(line)
                            }

                            else -> {
                                val equalsIndex = trimmedLine.indexOf('=')
                                if (equalsIndex > 0) {
                                    val key = trimmedLine.substring(0, equalsIndex).trim()
                                    existingKeys.add(key)
                                    config[key]?.let { value ->
                                        linesToPreserve.add("$key=$value")
                                    } ?: run {
                                        // Удаляем строки с удаленными ключами
                                        // Не добавляем их в linesToPreserve
                                    }
                                } else {
                                    linesToPreserve.add(line)
                                }
                            }
                        }
                    }
                }

                // Добавляем новые ключи
                val newKeys = config.keys - existingKeys
                if (newKeys.isNotEmpty()) {
                    if (linesToPreserve.isNotEmpty() && !linesToPreserve.last().isBlank()) {
                        linesToPreserve.add("")
                    }
                    linesToPreserve.add("# Added automatically")
                    newKeys.sorted().forEach { key ->
                        config[key]?.let { value ->
                            linesToPreserve.add("$key=$value")
                        }
                    }
                }

                writer.write(linesToPreserve.joinToString("\n"))
            }
        } catch (e: IOException) {
            throw RuntimeException("Save config failed", e)
        }
    }

    private inner class ConfigProperty<T>(
        private val key: String,
        private val defaultValue: T
    ) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return when (defaultValue) {
                is String -> getString(key, defaultValue as String) as T
                is Boolean -> getBoolean(key, defaultValue as Boolean) as T
                is Int -> getInt(key, defaultValue as Int) as T
                is Long -> getLong(key, defaultValue as Long) as T
                is Float -> getFloat(key, defaultValue as Float) as T
                is Set<*> -> getStringSet(key, defaultValue as Set<String>) as T
                else -> throw IllegalArgumentException("Unsupported type")
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            edit().apply {
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value as Set<String>)
                    else -> throw IllegalArgumentException("Unsupported type")
                }
            }.apply()
        }
    }
}

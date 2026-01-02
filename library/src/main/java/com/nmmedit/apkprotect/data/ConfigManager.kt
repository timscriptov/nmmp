package com.nmmedit.apkprotect.data

import com.nmmedit.apkprotect.util.FileUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ConfigManager private constructor() {
    companion object {
        private const val CONFIG_FILENAME = "config.txt"
        private const val CONFIG_DIR_NAME = "tools"
        private const val COMMENT_PREFIX = "#"
        private const val LINE_COMMENT_PREFIX = "//"

        val instance: ConfigManager by lazy { ConfigManager() }
    }

    private val configFileName: String
        get() = CONFIG_FILENAME

    private val configDirPath: String
        get() = File(FileUtils.getHomePath(), CONFIG_DIR_NAME).absolutePath

    private val configFilePath: String
        get() = File(configDirPath, configFileName).absolutePath

    @Volatile
    private var configCache: Map<String, String>? = null

    private val editor: Editor by lazy { Editor() }

    init {
        ensureConfigDirectoryExists()
    }

    private fun ensureConfigDirectoryExists() {
        try {
            val configDir = File(configDirPath)
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
        } catch (e: Exception) {
            println("Warning: Could not create config directory: ${e.message}")
        }
    }

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
        getConfigValue(key, defaultValue.toString())
            .takeIf { it.isNotBlank() }
            ?.toBooleanStrictOrNull()
            ?: defaultValue

    fun getInt(key: String, defaultValue: Int = 0): Int =
        getConfigValue(key, defaultValue.toString())
            .takeIf { it.isNotBlank() }
            ?.toIntOrNull()
            ?: defaultValue

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        getConfigValue(key, defaultValue.toString())
            .takeIf { it.isNotBlank() }
            ?.toLongOrNull()
            ?: defaultValue

    fun getFloat(key: String, defaultValue: Float = 0f): Float =
        getConfigValue(key, defaultValue.toString())
            .takeIf { it.isNotBlank() }
            ?.toFloatOrNull()
            ?: defaultValue

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> =
        getConfigValue(key, "")
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: defaultValue

    fun contains(key: String): Boolean = key in safeLoadConfig()

    fun getAll(): Map<String, String> = safeLoadConfig()

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
                    if (!configFile.exists()) {
                        configFile.createNewFile()
                    }

                    val config = safeLoadConfig().toMutableMap()

                    edits.forEach { (key, value) ->
                        when (value) {
                            null -> config.remove(key)
                            is Set<*> -> config[key] = (value as Set<String>).joinToString(",")
                            else -> config[key] = value.toString()
                        }
                    }

                    saveConfigToFile(configFile, config)
                    configCache = config
                    edits.clear()
                }
                true
            } catch (e: Exception) {
                println("Error committing config changes: ${e.message}")
                e.printStackTrace()
                false
            }
        }

        fun apply() {
            try {
                commit()
            } catch (e: Exception) {
                println("Error applying config changes: ${e.message}")
            }
        }
    }

    private fun safeLoadConfig(): Map<String, String> {
        return try {
            loadConfig()
        } catch (e: Exception) {
            println("Error loading config, returning empty map: ${e.message}")
            emptyMap()
        }
    }

    private fun loadConfig(): Map<String, String> {
        return configCache ?: synchronized(this) {
            configCache ?: loadConfigInternal().also { configCache = it }
        }
    }

    private fun loadConfigInternal(): Map<String, String> {
        val configFile = File(configFilePath)

        if (!configFile.exists()) {
            return emptyMap()
        }

        if (!configFile.canRead()) {
            println("Config file is not readable: $configFilePath")
            return emptyMap()
        }

        return try {
            parseConfigFile(configFile)
        } catch (e: Exception) {
            println("Error parsing config file: ${e.message}")
            emptyMap()
        }
    }

    private fun parseConfigFile(configFile: File): Map<String, String> {
        val config = mutableMapOf<String, String>()

        configFile.useLines { lines ->
            lines.forEachIndexed { index, line ->
                try {
                    val trimmedLine = line.trim()
                    when {
                        trimmedLine.isEmpty() -> return@forEachIndexed
                        trimmedLine.startsWith(COMMENT_PREFIX) -> return@forEachIndexed
                        trimmedLine.startsWith(LINE_COMMENT_PREFIX) -> return@forEachIndexed
                        else -> {
                            val equalsIndex = trimmedLine.indexOf('=')
                            if (equalsIndex > 0) {
                                val key = trimmedLine.substring(0, equalsIndex).trim()
                                val value = trimmedLine.substring(equalsIndex + 1).trim()
                                if (key.isNotBlank()) {
                                    config[key] = value
                                }
                            } else {
                                println("Warning: Invalid config line ${index + 1}: '$line'")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Warning: Error parsing line ${index + 1}: ${e.message}")
                }
            }
        }

        return config
    }

    private fun getConfigValue(key: String, defaultValue: String = ""): String {
        return try {
            val config = safeLoadConfig()
            config[key] ?: defaultValue
        } catch (e: Exception) {
            println("Error getting config value for key '$key': ${e.message}")
            defaultValue
        }
    }

    private fun saveConfigToFile(configFile: File, config: Map<String, String>) {
        try {
            val tempFile = File(configFile.parent, "${configFile.name}.tmp")
            tempFile.bufferedWriter().use { writer ->
                val linesToPreserve = mutableListOf<String>()
                val existingKeys = mutableSetOf<String>()
                if (configFile.exists() && configFile.canRead()) {
                    configFile.forEachLine { line ->
                        val trimmedLine = line.trim()
                        when {
                            trimmedLine.isEmpty() -> {
                                linesToPreserve.add(line)
                            }

                            trimmedLine.startsWith(COMMENT_PREFIX) || trimmedLine.startsWith(LINE_COMMENT_PREFIX) -> {
                                linesToPreserve.add(line)
                            }

                            else -> {
                                val equalsIndex = trimmedLine.indexOf('=')
                                if (equalsIndex > 0) {
                                    val key = trimmedLine.substring(0, equalsIndex).trim()
                                    existingKeys.add(key)
                                    config[key]?.let { value ->
                                        linesToPreserve.add("$key=$value")
                                    }
                                } else {
                                    linesToPreserve.add(line)
                                }
                            }
                        }
                    }
                }

                val newKeys = config.keys - existingKeys
                if (newKeys.isNotEmpty()) {
                    if (linesToPreserve.isNotEmpty() && linesToPreserve.last().isNotBlank()) {
                        linesToPreserve.add("")
                    }
                    newKeys.sorted().forEach { key ->
                        config[key]?.let { value ->
                            linesToPreserve.add("$key=$value")
                        }
                    }
                }

                writer.write(linesToPreserve.joinToString("\n"))
            }

            if (configFile.exists()) {
                configFile.delete()
            }
            tempFile.renameTo(configFile)

        } catch (e: Exception) {
            println("Error saving config file: ${e.message}")
            throw RuntimeException("Save config failed", e)
        }
    }

    internal fun clearCache() {
        configCache = null
    }

    private inner class ConfigProperty<T>(
        private val key: String,
        private val defaultValue: T
    ) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return try {
                when (defaultValue) {
                    is String -> getString(key, defaultValue as String) as T
                    is Boolean -> getBoolean(key, defaultValue as Boolean) as T
                    is Int -> getInt(key, defaultValue as Int) as T
                    is Long -> getLong(key, defaultValue as Long) as T
                    is Float -> getFloat(key, defaultValue as Float) as T
                    is Set<*> -> getStringSet(key, defaultValue as Set<String>) as T
                    else -> throw IllegalArgumentException("Unsupported type: $defaultValue")
                }
            } catch (e: Exception) {
                println("Error getting property '$key': ${e.message}")
                defaultValue
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            try {
                edit().apply {
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is Set<*> -> putStringSet(key, value as Set<String>)
                        else -> throw IllegalArgumentException("Unsupported type: $value")
                    }
                }.apply()
            } catch (e: Exception) {
                println("Error setting property '$key': ${e.message}")
            }
        }
    }
}

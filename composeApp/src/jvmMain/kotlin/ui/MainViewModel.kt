package ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.nmmedit.apkprotect.data.Prefs
import data.model.FieldType
import data.model.MainScreenState
import data.model.MainScreenValidator
import data.repository.MainRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import task.AabVmpTask
import task.AarVmpTask
import task.ApkVmpTask
import java.io.File

class MainViewModel(
    private val mainRepository: MainRepository,
) : ScreenModel {
    private val _exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private val _screenState = MutableStateFlow(
        MainScreenState(
            isArm7 = Prefs.isArm,
            isArm64 = Prefs.isArm64,
            isX86 = Prefs.isX86,
            isX64 = Prefs.isX64,
            rulesFilePath = Prefs.rulesPath,
            mappingFilePath = Prefs.mappingPath,
            ndkFilePath = Prefs.ndkPath,
            cMakeFilePath = Prefs.cmakePath,
            ndkStripBinaryFilePath = Prefs.ndkStripBinary,
            cxxFlags = Prefs.cxxFlags,
        )
    )
    val screenState = _screenState.asStateFlow()

    private val inputFilePath: String get() = _screenState.value.inputFilePath
    private val rulesPath: String get() = _screenState.value.rulesFilePath
    private val mappingPath: String get() = _screenState.value.mappingFilePath

    init {
        validateAll()
    }

    fun setInputFilePath(path: String) {
        _screenState.update {
            it.copy(
                inputFilePath = path.replace("\"", ""),
                validationErrors = validateField(path, FieldType.INPUT_FILE)
            )
        }
    }

    fun setRulesFilePath(path: String) {
        Prefs.rulesPath = path
        _screenState.update {
            it.copy(
                rulesFilePath = path.replace("\"", ""),
                validationErrors = validateField(path, FieldType.RULES_FILE)
            )
        }
    }

    fun setSdkFilePath(path: String) {
        Prefs.sdkPath = path
        _screenState.update {
            it.copy(
                sdkFilePath = path.replace("\"", ""),
                validationErrors = validateField(path, FieldType.SDK_DIRECTORY)
            )
        }
    }

    fun setNdkFilePath(path: String) {
        Prefs.ndkPath = path
        _screenState.update {
            it.copy(
                ndkFilePath = path.replace("\"", ""),
                validationErrors = validateField(path, FieldType.NDK_DIRECTORY)
            )
        }
    }

    fun setNdkStripBinaryFilePath(path: String) {
        Prefs.ndkStripBinary = path
        _screenState.update {
            it.copy(
                ndkStripBinaryFilePath = path.replace("\"", ""),
                validationErrors = validateField(path, FieldType.NDK_STRIP_BINARY_FILE)
            )
        }
    }

    fun setCMakeFilePath(path: String) {
        Prefs.cmakePath = path
        _screenState.update {
            it.copy(
                cMakeFilePath = path.replace("\"", ""),
                validationErrors = validateField(path, FieldType.CMAKE_DIRECTORY)
            )
        }
    }

    fun setCxxFlags(flags: String) {
        Prefs.cxxFlags = flags
        _screenState.update {
            it.copy(
                cxxFlags = flags,
            )
        }
    }

    fun setMappingFilePath(path: String) {
        Prefs.mappingPath = path
        _screenState.update {
            it.copy(
                mappingFilePath = path.replace("\"", "")
            )
        }
    }

    fun setArm7(mode: Boolean) {
        Prefs.isArm = mode
        _screenState.update {
            it.copy(
                isArm7 = mode
            )
        }
    }

    fun setArm64(mode: Boolean) {
        Prefs.isArm64 = mode
        _screenState.update {
            it.copy(
                isArm64 = mode
            )
        }
    }

    fun setX86(mode: Boolean) {
        Prefs.isX86 = mode
        _screenState.update {
            it.copy(
                isX86 = mode
            )
        }
    }

    fun setX64(mode: Boolean) {
        Prefs.isX64 = mode
        _screenState.update {
            it.copy(
                isX64 = mode
            )
        }
    }

    private fun validateField(value: String, fieldType: FieldType): Map<FieldType, String> {
        val result = when (fieldType) {
            FieldType.INPUT_FILE -> MainScreenValidator.validateInputFile(value)
            FieldType.RULES_FILE -> MainScreenValidator.validateRulesFile(value)
            FieldType.MAPPING_FILE -> MainScreenValidator.validateMappingFile(value)
            FieldType.SDK_DIRECTORY -> MainScreenValidator.validateSdkDirectory(value)
            FieldType.NDK_DIRECTORY -> MainScreenValidator.validateNdkDirectory(value)
            FieldType.NDK_STRIP_BINARY_FILE -> MainScreenValidator.validateNdkStripBinaryFile(value)
            FieldType.CMAKE_DIRECTORY -> MainScreenValidator.validateCmakeDirectory(value)
        }

        val currentErrors = _screenState.value.validationErrors.toMutableMap()
        if (result.isValid) {
            currentErrors.remove(fieldType)
        } else {
            currentErrors[fieldType] = result.errorMessage ?: ""
        }

        return currentErrors
    }

    private fun updateStateWithValidation(update: (MainScreenState) -> MainScreenState) {
        _screenState.value = update(_screenState.value)
    }

    fun validateAll(): Boolean {
        val validationResults = MainScreenValidator.validateAllFields(_screenState.value)
        val errors = validationResults.mapNotNull { (fieldType, result) ->
            if (!result.isValid) {
                fieldType to (result.errorMessage ?: "")
            } else {
                null
            }
        }.toMap()

        _screenState.value = _screenState.value.copy(validationErrors = errors)
        return errors.isEmpty()
    }

    fun startProcessing() {
        if (!validateAll()) {
            return
        }

        screenModelScope.launch(Dispatchers.IO + _exceptionHandler) {
            _screenState.update { it.copy(isLoading = true) }

            try {
                when {
                    inputFilePath.endsWith(".apk", ignoreCase = true) -> {
                        val output = File(inputFilePath.replace(Regex("(?i)\\.apk$"), "_vmp.apk"))
                        processApk(output)
                    }

                    inputFilePath.endsWith(".aab", ignoreCase = true) -> {
                        val output = File(inputFilePath.replace(Regex("(?i)\\.aab$"), "_vmp.aab"))
                        processAab(output)
                    }

                    inputFilePath.endsWith(".aar", ignoreCase = true) -> {
                        val output = File(inputFilePath.replace(Regex("(?i)\\.aar$"), "_vmp.aar"))
                        processAar(output)
                    }

                    else -> throw IllegalArgumentException("Unsupported file format. Use APK, AAB, or AAR.")
                }

                _screenState.update {
                    it.copy(
                        isLoading = false,
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _screenState.update {
                    it.copy(
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun processApk(outputFile: File) {
        ApkVmpTask(
            input = inputFilePath,
            output = outputFile.absolutePath,
            rules = rulesPath,
            mapping = mappingPath,
        ).start()
    }

    private fun processAab(outputFile: File) {
        AabVmpTask(
            input = inputFilePath,
            output = outputFile.absolutePath,
            rules = rulesPath,
            mapping = mappingPath,
        ).start()
    }

    private fun processAar(outputFile: File) {
        AarVmpTask(
            input = inputFilePath,
            output = outputFile.absolutePath,
            rules = rulesPath,
            mapping = mappingPath,
        ).start()
    }
}

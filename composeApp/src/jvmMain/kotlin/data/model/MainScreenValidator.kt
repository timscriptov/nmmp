package data.model

import java.io.File

object MainScreenValidator {

    fun validateAllFields(state: MainScreenState): Map<FieldType, ValidationResult> {
        return mapOf(
            FieldType.INPUT_FILE to validateInputFile(state.inputFilePath),
            FieldType.RULES_FILE to validateRulesFile(state.rulesFilePath),
            FieldType.MAPPING_FILE to validateMappingFile(state.mappingFilePath),
            FieldType.SDK_DIRECTORY to validateSdkDirectory(state.sdkFilePath),
            FieldType.NDK_DIRECTORY to validateNdkDirectory(state.ndkFilePath),
            FieldType.NDK_STRIP_BINARY_FILE to validateNdkStripBinaryFile(state.ndkStripBinaryFilePath),
            FieldType.CMAKE_DIRECTORY to validateCmakeDirectory(state.cMakeFilePath)
        )
    }

    fun validateInputFile(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Invalid(
                ValidationError.EMPTY_INPUT_FILE,
                ValidationError.getMessage(ValidationError.EMPTY_INPUT_FILE)
            )

            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.INPUT_FILE_NOT_FOUND,
                ValidationError.getMessage(ValidationError.INPUT_FILE_NOT_FOUND)
            )

            !hasValidInputExtension(path) -> ValidationResult.Invalid(
                ValidationError.INVALID_INPUT_EXTENSION,
                ValidationError.getMessage(ValidationError.INVALID_INPUT_EXTENSION)
            )

            else -> ValidationResult.Valid
        }
    }

    fun validateRulesFile(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Invalid(
                ValidationError.EMPTY_RULES_FILE,
                ValidationError.getMessage(ValidationError.EMPTY_RULES_FILE)
            )

            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.RULES_FILE_NOT_FOUND,
                ValidationError.getMessage(ValidationError.RULES_FILE_NOT_FOUND)
            )

            !path.endsWith(".txt", ignoreCase = true) -> ValidationResult.Invalid(
                ValidationError.INVALID_RULES_EXTENSION,
                ValidationError.getMessage(ValidationError.INVALID_RULES_EXTENSION)
            )

            else -> ValidationResult.Valid
        }
    }

    fun validateMappingFile(path: String): ValidationResult {
        // Mapping файл опциональный, если путь пустой - считаем валидным
        if (path.isBlank()) return ValidationResult.Valid

        return when {
            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.MAPPING_FILE_NOT_FOUND,
                ValidationError.getMessage(ValidationError.MAPPING_FILE_NOT_FOUND)
            )

            !path.endsWith(".txt", ignoreCase = true) -> ValidationResult.Invalid(
                ValidationError.INVALID_MAPPING_EXTENSION,
                ValidationError.getMessage(ValidationError.INVALID_MAPPING_EXTENSION)
            )

            else -> ValidationResult.Valid
        }
    }

    fun validateSdkDirectory(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Valid // SDK опциональный
            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.SDK_PATH_NOT_FOUND,
                ValidationError.getMessage(ValidationError.SDK_PATH_NOT_FOUND)
            )

            !File(path).isDirectory -> ValidationResult.Invalid(
                ValidationError.NOT_A_DIRECTORY_SDK,
                ValidationError.getMessage(ValidationError.NOT_A_DIRECTORY_SDK)
            )

            else -> ValidationResult.Valid
        }
    }

    fun validateNdkDirectory(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Invalid(
                ValidationError.EMPTY_NDK_PATH,
                ValidationError.getMessage(ValidationError.EMPTY_NDK_PATH)
            )

            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.NDK_PATH_NOT_FOUND,
                ValidationError.getMessage(ValidationError.NDK_PATH_NOT_FOUND)
            )

            !File(path).isDirectory -> ValidationResult.Invalid(
                ValidationError.NOT_A_DIRECTORY_NDK,
                ValidationError.getMessage(ValidationError.NOT_A_DIRECTORY_NDK)
            )

            else -> ValidationResult.Valid
        }
    }

    fun validateNdkStripBinaryFile(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Invalid(
                ValidationError.EMPTY_NDK_STRIP_BINARY,
                ValidationError.getMessage(ValidationError.EMPTY_NDK_STRIP_BINARY)
            )

            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.NDK_STRIP_BINARY_NOT_FOUND,
                ValidationError.getMessage(ValidationError.NDK_STRIP_BINARY_NOT_FOUND)
            )

            !File(path).isFile -> ValidationResult.Invalid(
                ValidationError.NOT_A_FILE_NDK_STRIP_BINARY,
                ValidationError.getMessage(ValidationError.NOT_A_FILE_NDK_STRIP_BINARY)
            )

            !File(path).canExecute() -> ValidationResult.Invalid(
                ValidationError.NDK_STRIP_BINARY_NOT_EXECUTABLE,
                ValidationError.getMessage(ValidationError.NDK_STRIP_BINARY_NOT_EXECUTABLE)
            )

            else -> ValidationResult.Valid
        }
    }

    fun validateCmakeDirectory(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Invalid(
                ValidationError.EMPTY_CMAKE_PATH,
                ValidationError.getMessage(ValidationError.EMPTY_CMAKE_PATH)
            )

            !File(path).exists() -> ValidationResult.Invalid(
                ValidationError.CMAKE_PATH_NOT_FOUND,
                ValidationError.getMessage(ValidationError.CMAKE_PATH_NOT_FOUND)
            )

            !File(path).isDirectory -> ValidationResult.Invalid(
                ValidationError.NOT_A_DIRECTORY_CMAKE,
                ValidationError.getMessage(ValidationError.NOT_A_DIRECTORY_CMAKE)
            )

            else -> ValidationResult.Valid
        }
    }

    private fun hasValidInputExtension(path: String): Boolean {
        val extensions = listOf(".apk", ".aab", ".aar")
        return extensions.any { path.endsWith(it, ignoreCase = true) }
    }
}

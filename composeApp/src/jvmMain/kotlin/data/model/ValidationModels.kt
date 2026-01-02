package data.model

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errorCode: ValidationError, val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val errorMessage: String? get() = (this as? Invalid)?.message
}

enum class ValidationError(val code: Int) {
    EMPTY_INPUT_FILE(1),
    INPUT_FILE_NOT_FOUND(2),
    INVALID_INPUT_EXTENSION(3),

    EMPTY_RULES_FILE(4),
    RULES_FILE_NOT_FOUND(5),
    INVALID_RULES_EXTENSION(6),

    MAPPING_FILE_NOT_FOUND(7),
    INVALID_MAPPING_EXTENSION(8),

    EMPTY_NDK_PATH(9),
    NDK_PATH_NOT_FOUND(10),
    NOT_A_DIRECTORY_NDK(11),

    EMPTY_CMAKE_PATH(12),
    CMAKE_PATH_NOT_FOUND(13),
    NOT_A_DIRECTORY_CMAKE(14),

    EMPTY_SDK_PATH(15),
    SDK_PATH_NOT_FOUND(16),
    NOT_A_DIRECTORY_SDK(17),

    EMPTY_NDK_STRIP_BINARY(18),
    NDK_STRIP_BINARY_NOT_FOUND(19),
    NOT_A_FILE_NDK_STRIP_BINARY(20),
    NDK_STRIP_BINARY_NOT_EXECUTABLE(21);

    companion object {
        private val messages = mapOf(
            EMPTY_INPUT_FILE to "Enter APK/AAB/AAR path",
            INPUT_FILE_NOT_FOUND to "APK/AAB/AAR file not found",
            INVALID_INPUT_EXTENSION to "File must be APK, AAB or AAR",

            EMPTY_RULES_FILE to "Enter rules path",
            RULES_FILE_NOT_FOUND to "Rules file not found",
            INVALID_RULES_EXTENSION to "Rules file must have .txt extension",

            MAPPING_FILE_NOT_FOUND to "Mapping file not found",
            INVALID_MAPPING_EXTENSION to "Mapping file must have .txt extension",

            EMPTY_NDK_PATH to "Enter Android NDK path",
            NDK_PATH_NOT_FOUND to "NDK directory not found",
            NOT_A_DIRECTORY_NDK to "NDK path must be a directory",

            EMPTY_CMAKE_PATH to "Enter CMake path",
            CMAKE_PATH_NOT_FOUND to "CMake directory not found",
            NOT_A_DIRECTORY_CMAKE to "CMake path must be a directory",

            EMPTY_SDK_PATH to "Enter Android SDK path",
            SDK_PATH_NOT_FOUND to "SDK directory not found",
            NOT_A_DIRECTORY_SDK to "SDK path must be a directory",

            EMPTY_NDK_STRIP_BINARY to "Enter NDK Strip Binary path",
            NDK_STRIP_BINARY_NOT_FOUND to "NDK Strip Binary file not found",
            NOT_A_FILE_NDK_STRIP_BINARY to "NDK Strip Binary path must be a file",
            NDK_STRIP_BINARY_NOT_EXECUTABLE to "NDK Strip Binary must be executable",
        )

        fun getMessage(error: ValidationError): String = messages[error] ?: "Unknown error"
    }
}

enum class FieldType {
    INPUT_FILE,      // APK/AAB/AAR
    RULES_FILE,      // .txt файл
    MAPPING_FILE,    // .txt файл
    SDK_DIRECTORY,   // Директория
    NDK_DIRECTORY,   // Директория
    NDK_STRIP_BINARY_FILE, // Исполняемый файл
    CMAKE_DIRECTORY  // Директория
}

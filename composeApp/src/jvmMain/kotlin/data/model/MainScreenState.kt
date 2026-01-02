package data.model

data class MainScreenState(
    val inputFilePath: String = "",
    val rulesFilePath: String = "",
    val mappingFilePath: String = "",
    val sdkFilePath: String = "",
    val ndkStripBinaryFilePath: String = "",
    val ndkFilePath: String = "",
    val cMakeFilePath: String = "",
    val isLoading: Boolean = false,
    val isArm7: Boolean = true,
    val isArm64: Boolean = true,
    val isX86: Boolean = true,
    val isX64: Boolean = true,
    val validationErrors: Map<FieldType, String> = emptyMap(),
) {
    fun isAllValid(): Boolean {
        return validationErrors.isEmpty() || validationErrors.all { it.value.isBlank() }
    }

    fun getInputFileError(): String? = validationErrors[FieldType.INPUT_FILE]
    fun getRulesFileError(): String? = validationErrors[FieldType.RULES_FILE]
    fun getMappingFileError(): String? = validationErrors[FieldType.MAPPING_FILE]
    fun getSdkPathError(): String? = validationErrors[FieldType.SDK_DIRECTORY]
    fun getNdkPathError(): String? = validationErrors[FieldType.NDK_DIRECTORY]
    fun getNdkStripBinaryPathError(): String? = validationErrors[FieldType.NDK_STRIP_BINARY_FILE]
    fun getCmakePathError(): String? = validationErrors[FieldType.CMAKE_DIRECTORY]
}
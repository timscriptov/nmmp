package utils

import java.io.File
import javax.swing.JFileChooser

object FilePicker {
    fun chooseFile(description: String, baseDirectory: String): String? {
        val fileChooser = JFileChooser(baseDirectory).apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = description
            approveButtonText = "Select"
            approveButtonToolTipText = description
        }
        fileChooser.showOpenDialog(null)
        val result = fileChooser.selectedFile
        return if (result != null && result.exists()) {
            result.absolutePath.toString()
        } else {
            null
        }
    }

    fun chooseDirectory(description: String, baseDirectory: String): String? {
        val fileChooser = JFileChooser(baseDirectory).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = description
            approveButtonText = "Select"
            approveButtonToolTipText = description
        }
        fileChooser.showOpenDialog(null)
        val result = fileChooser.selectedFile
        return if (result != null && result.exists()) {
            result.absolutePath.toString()
        } else {
            null
        }
    }

    fun getParentDirectory(path: String): String {
        val file = File(path)
        return if (file.exists()) {
            if (file.isDirectory) {
                file.absolutePath
            } else {
                file.parent
            }
        } else {
            "/"
        }
    }
}
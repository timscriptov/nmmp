import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mcal.preferences.Preferences
import com.nmmedit.apkprotect.data.Prefs
import com.nmmedit.apkprotect.data.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import task.*
import java.awt.Dimension
import java.io.File
import java.io.IOException
import javax.swing.JFileChooser

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePathInputField(
    modifier: Modifier,
    inputHint: String,
    selectionDescription: String,
    inputValue: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = inputValue,
        label = { Text(inputHint) },
        onValueChange = onValueChange,
        trailingIcon = {
            Button(
                modifier = Modifier.padding(5.dp),
                onClick = {
                    chooseFile(selectionDescription, getParentDirectory(inputValue))?.let { onValueChange(it) }
                }) {
                Text("select")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryPathInputField(
    modifier: Modifier,
    hintText: String,
    selectText: String,
    inputValue: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = inputValue,
        label = { Text(hintText) },
        onValueChange = onValueChange,
        trailingIcon = {
            Button(
                modifier = Modifier.padding(5.dp),
                onClick = {
                    chooseDirectory(selectText, getParentDirectory(inputValue))?.let { onValueChange(it) }
                }) {
                Text("select")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    var isEnabled by remember { mutableStateOf(true) }

    var inputFilePath by remember { mutableStateOf("") }

    var arm by remember { mutableStateOf(Prefs.isArm()) }
    var arm64 by remember { mutableStateOf(Prefs.isArm64()) }
    var x86 by remember { mutableStateOf(Prefs.isX86()) }
    var x64 by remember { mutableStateOf(Prefs.isX64()) }

    var rulesPath by remember { mutableStateOf("") }
    var mappingPath by remember { mutableStateOf("") }
    var sdkPath by remember { mutableStateOf(Prefs.getSdkPath()) }
    var ndkPath by remember { mutableStateOf(Prefs.getNdkPath()) }
    var cmakePath by remember { mutableStateOf(Prefs.getCmakePath()) }

    var vmName by remember { mutableStateOf(Prefs.getVmName()) }
    var nmmpName by remember { mutableStateOf(Prefs.getNmmpName()) }
    var className by remember { mutableStateOf(Prefs.getRegisterNativesClassName()) }

    var keystorePath by remember { mutableStateOf("") }
    var keystorePassword by remember { mutableStateOf("") }
    var keystoreAlias by remember { mutableStateOf("") }
    var keystoreAliasPassword by remember { mutableStateOf("") }

    val logs = remember { mutableStateListOf<String>() }

    MaterialTheme {
        Scaffold {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 16.dp, top = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter APK/AAR/AAB path*",
                                selectionDescription = "Select APK/AAR/AAB",
                                inputValue = inputFilePath,
                                onValueChange = {
                                    inputFilePath = it.replace("\"", "")
                                }
                            )
                            Button(
                                modifier = Modifier.padding(top = 8.dp),
                                enabled = isEnabled,
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        isEnabled = false
                                        if (inputFilePath.isEmpty()) {
                                            logs.add("W: Enter APK/AAB/AAR path")
                                        } else if (!File(inputFilePath).exists()) {
                                            logs.add("W: APK/AAB/AAR file not found")
                                        } else if (rulesPath.isEmpty()) {
                                            logs.add("W: Enter rules path")
                                        } else if (!File(rulesPath).exists()) {
                                            logs.add("W: Rules file not found")
                                        } else {
                                            logs.add("I: Starting...")

                                            val output: File
                                            try {
                                                if (inputFilePath.endsWith(".apk")) {
                                                    output = File(inputFilePath.replace(".apk", "_vmp.apk"))
                                                    ApkVmpTask(
                                                        input = inputFilePath,
                                                        output = output.path,
                                                        rules = rulesPath,
                                                        mapping = mappingPath,
                                                        logs = logs,
                                                    ).start()

                                                    logs.add("I: Start sign apk.")

                                                    ApkSignTask(
                                                        output = output.path,
                                                        keystorePath = keystorePath,
                                                        keystorePassword = keystorePassword,
                                                        keystoreAlias = keystoreAlias,
                                                        keystoreAliasPassword = keystoreAliasPassword,
                                                        logs = logs,
                                                    ).start()

                                                    logs.add("I: sign finished.")

                                                    logs.add("I: APK saved:\n${output.path}")
                                                } else if (inputFilePath.endsWith(".aab")) {
                                                    output = File(inputFilePath.replace(".aab", "_vmp.aab"))
                                                    AabVmpTask(
                                                        input = inputFilePath,
                                                        output = output.path,
                                                        rules = rulesPath,
                                                        mapping = mappingPath,
                                                        logs = logs,
                                                    ).start()

                                                    logs.add("I: Start sign aab.")

                                                    AabSignTask(
                                                        output = output.path,
                                                        keystorePath = keystorePath,
                                                        keystorePassword = keystorePassword,
                                                        keystoreAlias = keystoreAlias,
                                                        keystoreAliasPassword = keystoreAliasPassword,
                                                        logs = logs,
                                                    ).start()

                                                    logs.add("I: sign finished.")

                                                    logs.add("I: AAB saved:\n${output.path}")
                                                } else if (inputFilePath.endsWith(".aar")) {
                                                    output = File(inputFilePath.replace(".aar", "_vmp.aar"))
                                                    AarVmpTask(
                                                        input = inputFilePath,
                                                        output = output.path,
                                                        rules = rulesPath,
                                                        mapping = mappingPath,
                                                        logs = logs
                                                    ).start()
                                                    logs.add("I: AAR saved:\n${output.path}")
                                                } else {
                                                    logs.add(" Unknown file. Please select APK/AAB/AAR")
                                                }
                                            } catch (io: IOException) {
                                                logs.add("E: " + io.message)
                                            }
                                        }
                                        isEnabled = true
                                    }
                                }
                            ) {
                                Text("Convert")
                            }
                        }
                    }
                    if (!inputFilePath.endsWith(".aar")) {
                        Card(modifier = Modifier.padding(top = 8.dp)) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                            ) {
                                Row {
                                    Text(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        text = "armeabi-v7a"
                                    )
                                    Switch(
                                        checked = arm,
                                        onCheckedChange = {
                                            arm = it
                                            Prefs.setArm(it)
                                        }
                                    )
                                }
                                Row {
                                    Text(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        text = "arm64-v8a"
                                    )
                                    Switch(
                                        checked = arm64,
                                        onCheckedChange = {
                                            arm64 = it
                                            Prefs.setArm64(it)
                                        }
                                    )
                                }
                                Row {
                                    Text(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        text = "x86"
                                    )
                                    Switch(
                                        checked = x86,
                                        onCheckedChange = {
                                            x86 = it
                                            Prefs.setX86(it)
                                        }
                                    )
                                }
                                Row {
                                    Text(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        text = "x86_64"
                                    )
                                    Switch(
                                        checked = x64,
                                        onCheckedChange = {
                                            x64 = it
                                            Prefs.setX64(it)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Card(modifier = Modifier.padding(top = 8.dp)) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter rules path*",
                                selectionDescription = "Select rules file path",
                                inputValue = rulesPath,
                                onValueChange = {
                                    rulesPath = it.replace("\"", "")
                                }
                            )
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter ProGuard mapping path",
                                selectionDescription = "Select ProGuard mapping file path",
                                inputValue = mappingPath,
                                onValueChange = {
                                    mappingPath = it.replace("\"", "")
                                }
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter Android SDK path*",
                                selectText = "Select Android SDK path",
                                inputValue = sdkPath,
                                onValueChange = {
                                    sdkPath = it.replace("\"", "").also { path ->
                                        Prefs.setSdkPath(path)
                                    }
                                }
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter Android NDK path*",
                                selectText = "Select Android NDK path",
                                inputValue = ndkPath,
                                onValueChange = {
                                    ndkPath = it.replace("\"", "").also { path ->
                                        Prefs.setNdkPath(path)
                                    }
                                }
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter CMake path*",
                                selectText = "Select CMake path",
                                inputValue = cmakePath,
                                onValueChange = {
                                    cmakePath = it.replace("\"", "").also { path ->
                                        Prefs.setCmakePath(path)
                                    }
                                }
                            )
                        }
                    }
                    Card(modifier = Modifier.padding(top = 8.dp)) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = vmName,
                                label = { Text("Enter vm name path*") },
                                onValueChange = {
                                    vmName = it.also { name ->
                                        Prefs.setVmName(name)
                                    }
                                }
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = nmmpName,
                                label = { Text("Enter nmmp name path*") },
                                onValueChange = {
                                    nmmpName = it.also { name ->
                                        Prefs.setNmmpName(name)
                                    }
                                }
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = className,
                                label = { Text("Enter class name path*") },
                                onValueChange = {
                                    className = it.also { name ->
                                        Prefs.setRegisterNativesClassName(name)
                                    }
                                }
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.padding(top = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter keystore path*",
                                selectionDescription = "Select skystore file path*",
                                inputValue = keystorePath,
                                onValueChange = {
                                    keystorePath = it.replace("\"", "")
                                }
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = keystorePassword,
                                label = { Text("Enter keystore password*") },
                                onValueChange = {
                                    keystorePassword = it.also { name ->
                                        Prefs.setRegisterNativesClassName(name)
                                    }
                                }
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = keystoreAlias,
                                label = { Text("Enter keystore alias*") },
                                onValueChange = {
                                    keystoreAlias = it.also { name ->
                                        Prefs.setRegisterNativesClassName(name)
                                    }
                                }
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = keystoreAliasPassword,
                                label = { Text("Enter keystore alias password*") },
                                onValueChange = {
                                    keystoreAliasPassword = it.also { name ->
                                        Prefs.setRegisterNativesClassName(name)
                                    }
                                }
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text("Copyright 2023 timscriptov")
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier
                                .padding(8.dp),
                            state = listState
                        ) {
                            if (logs.isNotEmpty()) {
                                items(logs) { log ->
                                    if (log.startsWith("W")) {
                                        Text(text = log, style = TextStyle(color = Color(0xFFFFA500)))
                                    } else if (log.startsWith("E")) {
                                        Text(text = log, style = TextStyle(color = Color.Red))
                                    } else {
                                        Text(log)
                                    }
                                    Divider(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                                    if (!isEnabled && logs.size > 1) {
                                        LaunchedEffect(listState) {
                                            listState.scrollToItem(logs.size - 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Preferences(Storage.binDir, "nmmp_preferences.json").init()
    Window(
        title = "VMP",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App()
    }
}

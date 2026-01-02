package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import composition.DirectoryPathInputField
import composition.FilePathInputField
import composition.SwitchText

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<MainViewModel>()
        val screenState by viewModel.screenState.collectAsState()

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 64.dp, vertical = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when {
                        screenState.isLoading -> {
                            CircularProgressIndicator()
                        }

                        else -> {
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter APK/AAR/AAB path*",
                                selectionDescription = "Select APK/AAR/AAB",
                                inputValue = screenState.inputFilePath,
                                onValueChange = viewModel::setInputFilePath,
                                errorMessage = screenState.getInputFileError(),
                                isRequired = true,
                            )
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter rules path*",
                                selectionDescription = "Select rules file path",
                                inputValue = screenState.rulesFilePath,
                                onValueChange = viewModel::setRulesFilePath,
                                errorMessage = screenState.getRulesFileError(),
                                isRequired = true,
                            )
                            FilePathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                inputHint = "Enter ProGuard mapping path",
                                selectionDescription = "Select ProGuard mapping file path",
                                inputValue = screenState.mappingFilePath,
                                onValueChange = viewModel::setMappingFilePath,
                                errorMessage = screenState.getMappingFileError(),
                                isRequired = false,
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter Android SDK path",
                                selectText = "Select Android SDK path",
                                inputValue = screenState.sdkFilePath,
                                onValueChange = viewModel::setSdkFilePath,
                                errorMessage = screenState.getSdkPathError(),
                                isRequired = false,
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter Android NDK path*",
                                selectText = "Select Android NDK path",
                                inputValue = screenState.ndkFilePath,
                                onValueChange = viewModel::setNdkFilePath,
                                errorMessage = screenState.getNdkPathError(),
                                isRequired = true,
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter NDK Strip Binary path*",
                                selectText = "Select NDK Strip Binary path",
                                inputValue = screenState.ndkStripBinaryFilePath,
                                onValueChange = viewModel::setNdkStripBinaryFilePath,
                                errorMessage = screenState.getNdkStripBinaryPathError(),
                                isRequired = true,
                            )
                            DirectoryPathInputField(
                                modifier = Modifier.fillMaxWidth(),
                                hintText = "Enter CMake path*",
                                selectText = "Select CMake path",
                                inputValue = screenState.cMakeFilePath,
                                onValueChange = viewModel::setCMakeFilePath,
                                errorMessage = screenState.getCmakePathError(),
                                isRequired = true,
                            )
                            SwitchText(
                                modifier = Modifier.fillMaxWidth(),
                                text = "armeabi-v7a",
                                checked = screenState.isArm7,
                                onCheckedChange = viewModel::setArm7,
                            )
                            SwitchText(
                                modifier = Modifier.fillMaxWidth(),
                                text = "arm64-v8a",
                                checked = screenState.isArm64,
                                onCheckedChange = viewModel::setArm64,
                            )
                            SwitchText(
                                modifier = Modifier.fillMaxWidth(),
                                text = "X86",
                                checked = screenState.isX86,
                                onCheckedChange = viewModel::setX86,
                            )
                            SwitchText(
                                modifier = Modifier.fillMaxWidth(),
                                text = "X64-86",
                                checked = screenState.isX64,
                                onCheckedChange = viewModel::setX64,
                            )
                            Button(
                                modifier = Modifier.padding(8.dp),
                                onClick = {
                                    if (viewModel.validateAll()) {
                                        viewModel.startProcessing()
                                    }
                                },
                                enabled = screenState.isAllValid()
                            ) {
                                Text(text = "Protect")
                            }
                        }
                    }
                }
            }
        )
    }
}
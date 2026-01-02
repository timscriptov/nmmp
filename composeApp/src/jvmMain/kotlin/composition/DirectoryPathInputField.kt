package composition

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nmmp_gui.composeapp.generated.resources.Res
import nmmp_gui.composeapp.generated.resources.folder_open_24px
import org.jetbrains.compose.resources.painterResource
import utils.FilePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryPathInputField(
    modifier: Modifier,
    hintText: String,
    selectText: String,
    inputValue: String,
    onValueChange: (String) -> Unit,
    errorMessage: String? = null,
    isRequired: Boolean = true
) {
    val supportingText = if (errorMessage != null) {
        @Composable {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else if (isRequired) {
        @Composable {
            Text(
                text = "* Required",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else {
        null
    }

    OutlinedTextField(
        modifier = modifier,
        value = inputValue,
        label = { Text(hintText) },
        onValueChange = onValueChange,
        shape = ShapeDefaults.Small,
        isError = errorMessage != null,
        supportingText = supportingText,
        trailingIcon = {
            IconButton(
                modifier = Modifier.padding(4.dp),
                onClick = {
                    FilePicker.chooseDirectory(
                        selectText,
                        FilePicker.getParentDirectory(inputValue)
                    )?.let { onValueChange(it) }
                }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.folder_open_24px),
                    contentDescription = "Select directory"
                )
            }
        }
    )
}

package composition

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import utils.FilePicker

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
        shape = ShapeDefaults.Small,
        trailingIcon = {
            IconButton(
                modifier = Modifier.padding(4.dp),
                onClick = {
                    FilePicker.chooseFile(
                        selectionDescription,
                        FilePicker.getParentDirectory(inputValue)
                    )?.let { onValueChange(it) }
                }
            ) {
                Icon(painter = painterResource("ic_folder_open.svg"), contentDescription = "Select file")
            }
        }
    )
}

package composition

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import utils.FilePicker

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
        shape = ShapeDefaults.Small,
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
                Icon(painter = painterResource("ic_folder_open.svg"), contentDescription = "Select directory")
            }
        }
    )
}

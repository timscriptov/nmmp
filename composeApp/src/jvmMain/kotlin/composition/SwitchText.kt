package composition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SwitchText(
    modifier: Modifier = Modifier,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .clickable {
                onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Spacer(
            modifier = Modifier.size(16.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            text = text,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(
            modifier = Modifier.size(16.dp)
        )
    }
}
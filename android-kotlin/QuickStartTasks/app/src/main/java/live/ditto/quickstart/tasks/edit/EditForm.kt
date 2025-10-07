package live.ditto.quickstart.tasks.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EditForm(
    canDelete: Boolean,
    title: String,
    onTitleTextChange: ((title: String) -> Unit)? = null,
    done: Boolean = false,
    onDoneChanged: ((done: Boolean) -> Unit)? = null,
    onDeleteButtonClicked: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { onTitleTextChange?.invoke(it) },
                    label = { Text("Title") },
                    placeholder = { Text("Enter task title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Is Complete",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = done,
                        onCheckedChange = { onDoneChanged?.invoke(it) }
                    )
                }
            }
        }

        if (canDelete) {
            Button(
                onClick = {
                    onDeleteButtonClicked?.invoke()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Delete",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3
)
@Composable
fun EditFormPreview() {
    EditForm(canDelete = true, "Hello")
}

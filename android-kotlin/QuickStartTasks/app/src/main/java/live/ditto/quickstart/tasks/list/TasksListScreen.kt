package live.ditto.quickstart.tasks.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.BuildConfig
import live.ditto.quickstart.tasks.R
import live.ditto.quickstart.tasks.data.Task
import java.util.UUID

@Composable
fun TasksListScreen(navController: NavController) {
    val tasksListViewModel: TasksListScreenViewModel = viewModel()
    val tasks: List<Task> by tasksListViewModel.tasks.observeAsState(emptyList())
    val syncEnabled: Boolean by tasksListViewModel.syncEnabled.observeAsState(true)
    val bluetoothEnabled: Boolean by tasksListViewModel.bluetoothEnabled.observeAsState(true)
    val lanEnabled: Boolean by tasksListViewModel.lanEnabled.observeAsState(true)
    val wifiAwareEnabled: Boolean by tasksListViewModel.wifiAwareEnabled.observeAsState(true)
    val count: Int by tasksListViewModel.count.observeAsState(0)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteDialogTaskId by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val backgroundColor = Color(getBackgroundColour(count))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor,
        bottomBar = {
            // Bottom button bar matching iOS style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
            ) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("tasks/bulkadd") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.blue_700)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Bulk Add",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Bulk Add")
                        }
                    }
                    Button(
                        onClick = { navController.navigate("tasks/edit") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.blue_700)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "New Task",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("New Task")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {
            // Transport controls row at the top
            TransportControlsRow(
                bluetoothEnabled = bluetoothEnabled,
                lanEnabled = lanEnabled,
                wifiAwareEnabled = wifiAwareEnabled,
                syncEnabled = syncEnabled,
                onBluetoothToggle = { tasksListViewModel.toggleBluetoothLE() },
                onLANToggle = { tasksListViewModel.toggleLAN() },
                onWifiAwareToggle = { tasksListViewModel.toggleWifiAware() },
                onSyncToggle = { tasksListViewModel.setSyncEnabled(!syncEnabled) },
                onToolsClick = { navController.navigate("tools") },
                onDeleteAllClick = { showDeleteAllDialog = true },
                backgroundColor = backgroundColor
            )

            // Count display
            Text(
                text = "$count",
                style = TextStyle(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
            )

            // Task list
            TasksList(
                tasks = tasks,
                count = count,
                onToggle = { tasksListViewModel.toggle(it) },
                onClickEdit = {
                    navController.navigate("tasks/edit/${it}")
                },
                onClickDelete = {
                    deleteDialogTaskId = it
                    showDeleteDialog = true
                }
            )
        }
    }

    // Alert displayed if user taps a Delete icon for a list item
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Confirm Deletion",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(text = "Are you sure you want to delete this item?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        tasksListViewModel.delete(deleteDialogTaskId)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Alert displayed if user taps the Delete All button
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete All Incomplete Tasks",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(text = "This will permanently delete all incomplete tasks. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        tasksListViewModel.deleteAllIncomplete()
                        tasksListViewModel.evictAllDeleted()
                    }
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransportControlsRow(
    bluetoothEnabled: Boolean,
    lanEnabled: Boolean,
    wifiAwareEnabled: Boolean,
    syncEnabled: Boolean,
    onBluetoothToggle: () -> Unit,
    onLANToggle: () -> Unit,
    onWifiAwareToggle: () -> Unit,
    onSyncToggle: () -> Unit,
    onToolsClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    backgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Bluetooth toggle
        TransportButton(
            imageVector = Icons.Filled.Bluetooth,
            label = "Bluetooth",
            enabled = bluetoothEnabled,
            onClick = onBluetoothToggle
        )

        // LAN/WiFi toggle
        TransportButton(
            imageVector = if (lanEnabled) Icons.Filled.Wifi else Icons.Filled.WifiOff,
            label = "WiFi",
            enabled = lanEnabled,
            onClick = onLANToggle
        )

        // WiFi Aware toggle
        TransportButton(
            imageVector = if (wifiAwareEnabled) Icons.Filled.Wifi else Icons.Filled.WifiOff,
            label = "WiFi Aware",
            enabled = wifiAwareEnabled,
            onClick = onWifiAwareToggle
        )

        Spacer(modifier = Modifier.weight(1f))

        // Sync toggle
        TransportButton(
            imageVector = Icons.Filled.Sync,
            label = "Sync",
            enabled = syncEnabled,
            onClick = onSyncToggle
        )

        // Ditto Tools button
        TransportButton(
            imageVector = Icons.Filled.Build,
            label = "Tools",
            enabled = true,
            onClick = onToolsClick
        )

        // Delete All button
        TransportButton(
            imageVector = Icons.Filled.Delete,
            label = "Delete All",
            enabled = true,
            onClick = onDeleteAllClick
        )
    }
}

@Composable
fun TransportButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (enabled) colorResource(id = R.color.blue_700) else Color.Gray.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(44.dp)
            ) {
                Box {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TasksList(
    tasks: List<Task>,
    count: Int,
    onToggle: ((taskId: String) -> Unit)? = null,
    onClickEdit: ((taskId: String) -> Unit)? = null,
    onClickDelete: ((taskId: String) -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(getBackgroundColour(count)))
    ) {
        items(tasks) { task ->
            TaskRow(
                task = task,
                backgroundColor = Color(getBackgroundColour(count)),
                onToggle = { onToggle?.invoke(it._id) },
                onClickEdit = { onClickEdit?.invoke(it._id) },
                onClickDelete = { onClickDelete?.invoke(it._id) }
            )
        }
    }
}

@Preview(
    showBackground = false,
    showSystemUi = true,
    device = Devices.PIXEL_3
)
@Composable
fun TasksListPreview() {
    TasksList(
        tasks = listOf(
            Task(UUID.randomUUID().toString(), "Get Milk", done = true, deleted = false),
            Task(UUID.randomUUID().toString(), "Get Oats", done = false, deleted = false),
            Task(UUID.randomUUID().toString(), "Get Berries", done = true, deleted = false),
        ),
        count = 3
    )
}

fun getBackgroundColour(taskNumber: Int): Int {
    val colorsHex = BuildConfig.COLORS_HEX
    val colorsList = colorsHex.split(",").map { it.trim() }

    val colorIndex = taskNumber % colorsList.size
    val hexColor = colorsList[colorIndex]

    return hexColor.toColorInt()
}
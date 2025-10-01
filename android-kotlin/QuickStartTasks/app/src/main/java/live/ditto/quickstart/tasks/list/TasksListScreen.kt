package live.ditto.quickstart.tasks.list

import android.graphics.fonts.Font
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.BuildConfig
import live.ditto.quickstart.tasks.R
import live.ditto.quickstart.tasks.data.Task
import java.util.UUID
import androidx.core.graphics.toColorInt
import org.koin.core.logger.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(navController: NavController, isDittoInitialized: Boolean = true) {
    val tasksListViewModel: TasksListScreenViewModel = viewModel()
    val tasks: List<Task> by tasksListViewModel.tasks.observeAsState(emptyList())
    val syncEnabled: Boolean by tasksListViewModel.syncEnabled.observeAsState(true)
    val count: Int by tasksListViewModel.count.observeAsState(0)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteDialogTaskId by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.height(150.dp),

                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(end = 80.dp) // Leave space for sync switch
                    ) {
                        Text(
                            text = "$count",
                            style = TextStyle(
                                fontSize = if (count.toString().length > 4) 48.sp else if (count.toString().length > 3) 60.sp else 72.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = if (count.toString().length > 4) 48.sp else if (count.toString().length > 3) 60.sp else 72.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterStart)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(getBackgroundColour(count)),
                    titleContentColor = Color.Black,

                ),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true }
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete All",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { navController.navigate("tools") }
                        ) {
                            Icon(
                                Icons.Filled.Build,
                                contentDescription = "Tools",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Sync",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 10.dp),
                            color = Color.White
                        )
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { isChecked ->
                                tasksListViewModel.setSyncEnabled(isChecked)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Filled.List, "", tint = Color.White) },
                    text = { Text(text = "Bulk Add", color = Color.White) },
                    onClick = { navController.navigate("tasks/bulkadd") },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    containerColor = colorResource(id = R.color.blue_500)
                )
                Spacer(modifier = Modifier.width(16.dp))
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Filled.Add, "", tint = Color.White) },
                    text = { Text(text = "New Task", color = Color.White) },
                    onClick = { navController.navigate("tasks/edit") },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    containerColor = colorResource(id = R.color.blue_500)
                )
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Transparent)
            ) {
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
    )

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
            Task(UUID.randomUUID().toString(), "Get Milk", true, false),
            Task(UUID.randomUUID().toString(), "Get Oats", false, false),
            Task(UUID.randomUUID().toString(), "Get Berries", true, false),
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
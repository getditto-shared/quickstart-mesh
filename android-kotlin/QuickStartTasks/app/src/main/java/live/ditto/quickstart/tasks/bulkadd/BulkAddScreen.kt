package live.ditto.quickstart.tasks.bulkadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddScreen(navController: NavController) {
    val bulkAddViewModel: BulkAddScreenViewModel = viewModel()
    
    var prefix by remember { mutableStateOf("") }
    var taskNumberText by remember { mutableStateOf("") }
    var delayText by remember { mutableStateOf("1000") } // Default 1000ms delay
    val focusRequester = remember { FocusRequester() }
    
    val taskNumber = taskNumberText.toIntOrNull() ?: 0
    val delay = delayText.toLongOrNull() ?: 1000L
    val isCreating by bulkAddViewModel.isCreating.observeAsState(false)
    val progress by bulkAddViewModel.progress.observeAsState("")
    val isCreateEnabled = taskNumber > 0 && !isCreating
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Tasks", color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            if (!isCreating) {
                                navController.popBackStack() 
                            }
                        },
                        enabled = !isCreating
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = if (isCreating) Color.Gray else Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (!isCreating) {
                                bulkAddViewModel.createTasks(prefix, taskNumber, delay)
                            }
                        },
                        enabled = isCreateEnabled
                    ) {
                        Text(
                            if (isCreating) "Creating..." else "Create",
                            color = if (isCreateEnabled) Color.White else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.blue_700)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
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
                        value = taskNumberText,
                        onValueChange = { taskNumberText = it.filter { char -> char.isDigit() } },
                        label = { Text("Number of Tasks") },
                        placeholder = { Text("Enter number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = delayText,
                        onValueChange = { delayText = it.filter { char -> char.isDigit() } },
                        label = { Text("Delay (milliseconds)") },
                        placeholder = { Text("Time between creates (1000 = 1 second)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("Prefix") },
                        placeholder = { Text("Optional prefix for tasks") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            if (isCreating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Creating Tasks...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Text(
                    text = if (taskNumber > 0) {
                        val taskPrefix = if (prefix.isNotEmpty()) "$prefix-" else "Task-"
                        val delaySeconds = delay / 1000.0
                        val totalTime = (taskNumber - 1) * delaySeconds
                        "Will create $taskNumber tasks:\n${taskPrefix}1, ${taskPrefix}2, ... ${taskPrefix}$taskNumber\n\nDelay: ${delaySeconds}s between each create\nTotal time: ~${totalTime}s"
                    } else {
                        "Enter the number of tasks to create"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Request focus on the number field when the screen appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Navigate back when creation is complete
    LaunchedEffect(isCreating) {
        if (!isCreating && progress.startsWith("Completed!")) {
            navController.popBackStack()
        }
    }
}
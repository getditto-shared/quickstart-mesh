package live.ditto.quickstart.tasks.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(navController: NavController, taskId: String?) {
    val editScreenViewModel: EditScreenViewModel = viewModel()
    editScreenViewModel.setupWithTask(id = taskId)

    val topBarTitle = if (taskId == null) "New Task" else "Edit Task"

    val title: String by editScreenViewModel.title.observeAsState("")
    val done: Boolean by editScreenViewModel.done.observeAsState(initial = false)
    val canDelete: Boolean by editScreenViewModel.canDelete.observeAsState(initial = false)

    val isSaveEnabled = title.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            editScreenViewModel.save()
                            navController.popBackStack()
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text(
                            "Save",
                            color = if (isSaveEnabled) Color.White else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.blue_700)
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EditForm(
                    canDelete = canDelete,
                    title = title,
                    onTitleTextChange = { editScreenViewModel.title.value = it },
                    done = done,
                    onDoneChanged = { editScreenViewModel.done.value = it },
                    onDeleteButtonClicked = {
                        editScreenViewModel.delete()
                        navController.popBackStack()
                    }
                )
            }
        }
    )
}

package live.ditto.quickstart.tasks.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import live.ditto.tools.toolsviewer.DittoToolsViewer
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DittoToolsScreen(navController: NavController) {
    val dittoManager = TasksApplication.getDittoManager()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ditto Tools", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.blue_700)
                )
            )
        }
    ) { paddingValues ->
        if (dittoManager.isDittoInitialized()) {
            DittoToolsViewer(
                ditto = dittoManager.requireDitto(),
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Initializing Ditto...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
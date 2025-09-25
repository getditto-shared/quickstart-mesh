package live.ditto.quickstart.tasks.tools

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import live.ditto.tools.toolsviewer.DittoToolsViewer
import live.ditto.quickstart.tasks.DittoHandler

@Composable
fun DittoToolsScreen(navController: NavController) {
    DittoToolsViewer(
        ditto = DittoHandler.ditto
    )
}
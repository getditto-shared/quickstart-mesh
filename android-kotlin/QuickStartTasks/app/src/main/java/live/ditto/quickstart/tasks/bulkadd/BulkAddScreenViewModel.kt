package live.ditto.quickstart.tasks.bulkadd

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.DittoError
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto

class BulkAddScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "BulkAddScreenViewModel"
    }

    fun createTasks(prefix: String, count: Int) {
        viewModelScope.launch {
            // Using a for loop to create tasks individually for stress testing
            // This simulates real-world scenarios where tasks are created one by one
            for (i in 1..count) {
                try {
                    val taskTitle = if (prefix.isNotEmpty()) "$prefix-$i" else "Task-$i"
                    
                    // Add tasks into the ditto collection using DQL INSERT statement
                    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                    ditto.store.execute(
                        "INSERT INTO tasks DOCUMENTS (:doc)",
                        mapOf(
                            "doc" to mapOf(
                                "title" to taskTitle,
                                "done" to false,
                                "deleted" to false
                            )
                        )
                    )
                } catch (e: DittoError) {
                    Log.e(TAG, "Unable to create task $i", e)
                }
            }
        }
    }
}
package live.ditto.quickstart.tasks.bulkadd

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoManager
import live.ditto.quickstart.tasks.TasksApplication

class BulkAddScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "BulkAddScreenViewModel"
    }

    private val dittoManager: DittoManager = TasksApplication.getDittoManager()
    
    // Use application scope so it survives ViewModel destruction
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isCreating = MutableLiveData(false)
    val isCreating: LiveData<Boolean> = _isCreating

    private val _progress = MutableLiveData("")
    val progress: LiveData<String> = _progress

    fun createTasks(prefix: String, count: Int, delayMs: Long = 1000L) {
        applicationScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot create tasks - Ditto not initialized")
                return@launch
            }

            _isCreating.postValue(true)
            Log.d(TAG, "Starting to create $count tasks with ${delayMs}ms delay between each")

            // Using a for loop to create tasks individually for stress testing
            // This simulates real-world scenarios where tasks are created one by one
            for (i in 1..count) {
                val taskTitle = if (prefix.isNotEmpty()) "$prefix-$i" else "Task-$i"
                
                _progress.postValue("Creating task $i of $count: $taskTitle")
                Log.d(TAG, "Creating task $i: $taskTitle")
                
                // Add tasks into the ditto collection using DQL INSERT statement
                // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                dittoManager.executeQuery(
                    "INSERT INTO tasks DOCUMENTS (:doc)",
                    mapOf(
                        "doc" to mapOf(
                            "title" to taskTitle,
                            "done" to false,
                            "deleted" to false
                        )
                    )
                ).onFailure { e ->
                    Log.e(TAG, "Unable to create task $i", e)
                }
                
                // Add delay between creates (except after the last one)
                if (i < count) {
                    kotlinx.coroutines.delay(delayMs)
                }
            }
            
            _progress.postValue("Completed! Created $count tasks")
            Log.d(TAG, "Finished creating $count tasks")
            
            // Wait a moment to show completion, then stop
            kotlinx.coroutines.delay(1000)
            _isCreating.postValue(false)
        }
    }
}
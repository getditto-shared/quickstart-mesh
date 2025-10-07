package live.ditto.quickstart.tasks.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.DittoError
import live.ditto.quickstart.tasks.DittoManager
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

// Removed DataStore persistence - transport states now default to true on app start

class TasksListScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "TasksListScreenViewModel"
        private const val QUERY = "SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC LIMIT 50"
        private const val COUNT_QUERY = "SELECT COUNT(*) as result FROM tasks WHERE NOT deleted"
    }

    private val dittoManager: DittoManager = TasksApplication.getDittoManager()

    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    private val _syncEnabled = MutableLiveData(true)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    private val _bluetoothEnabled = MutableLiveData(true)
    val bluetoothEnabled: LiveData<Boolean> = _bluetoothEnabled

    private val _lanEnabled = MutableLiveData(true)
    val lanEnabled: LiveData<Boolean> = _lanEnabled

    private val _wifiAwareEnabled = MutableLiveData(true)
    val wifiAwareEnabled: LiveData<Boolean> = _wifiAwareEnabled

    val count: MutableLiveData<Int> = MutableLiveData(0)

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _syncEnabled.value = enabled

            if (enabled && !dittoManager.isSyncActive()) {
                try {
                    dittoManager.startSync()
                } catch (e: DittoError) {
                    Log.e(TAG, "Unable to start sync", e)
                }
            } else if (!enabled && dittoManager.isSyncActive()) {
                try {
                    dittoManager.stopSync()
                } catch (e: DittoError) {
                    Log.e(TAG, "Unable to stop sync", e)
                }
            }
        }
    }

    fun toggleBluetoothLE() {
        val newValue = !(_bluetoothEnabled.value ?: true)
        _bluetoothEnabled.value = newValue
        dittoManager.toggleBluetoothLE()
    }

    fun toggleLAN() {
        val newValue = !(_lanEnabled.value ?: true)
        _lanEnabled.value = newValue
        dittoManager.toggleLAN()
    }

    fun toggleWifiAware() {
        val newValue = !(_wifiAwareEnabled.value ?: true)
        _wifiAwareEnabled.value = newValue
        dittoManager.toggleWifiAware()
    }

    init {
        viewModelScope.launch {
            // Wait for Ditto to be initialized before setting up observers
            while (!dittoManager.isDittoInitialized()) {
                kotlinx.coroutines.delay(100) // Wait 100ms before checking again
            }

            populateTasksCollection()
            setupObservers()

            // Start sync by default (all transports default to enabled)
            setSyncEnabled(true)
        }
    }

    private fun setupObservers() {
        viewModelScope.launch {
            // Observe tasks using Flow
            dittoManager.liveQueryAsFlow(QUERY, emptyMap()).collect { result ->
                val list = result.items.map { item -> Task.fromJson(item.jsonString()) }
                tasks.postValue(list)
            }
        }

        viewModelScope.launch {
            // Observe count using Flow
            dittoManager.liveQueryAsFlow(COUNT_QUERY, emptyMap()).collect { result ->
                result.items.forEach {
                    count.postValue(it.value["result"] as Int)
                }
            }
        }
    }

    // Add initial tasks to the collection if they have not already been added.
    private fun populateTasksCollection() {
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot populate tasks - Ditto not initialized")
                return@launch
            }

            val tasks = listOf(
                Task("50191411-4C46-4940-8B72-5F8017A04FA7", "Buy groceries"),
                Task("6DA283DA-8CFE-4526-A6FA-D385089364E5", "Clean the kitchen"),
                Task("5303DDF8-0E72-4FEB-9E82-4B007E5797F0", "Schedule dentist appointment"),
                Task("38411F1B-6B49-4346-90C3-0B16CE97E174", "Pay bills")
            )

            tasks.forEach { task ->
                dittoManager.executeQuery(
                    "INSERT INTO tasks INITIAL DOCUMENTS (:task)",
                    mapOf(
                        "task" to mapOf(
                            "_id" to task._id,
                            "title" to task.title,
                            "done" to task.done,
                            "deleted" to task.deleted,
                        )
                    )
                ).onFailure { e ->
                    Log.e(TAG, "Unable to insert initial document", e)
                }
            }
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot toggle task - Ditto not initialized")
                return@launch
            }

            try {
                val docResult = dittoManager.executeQuery(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId)
                )

                docResult.onSuccess { result ->
                    val doc = result.items.first()
                    val done = doc.value["done"] as Boolean

                    dittoManager.executeQuery(
                        "UPDATE tasks SET done = :toggled WHERE _id = :_id AND NOT deleted",
                        mapOf(
                            "toggled" to !done,
                            "_id" to taskId
                        )
                    ).onFailure { e ->
                        Log.e(TAG, "Unable to toggle done state", e)
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Unable to find task", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to toggle done state", e)
            }
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot delete task - Ditto not initialized")
                return@launch
            }

            dittoManager.executeQuery(
                "UPDATE tasks SET deleted = true WHERE _id = :id",
                mapOf("id" to taskId)
            ).onFailure { e ->
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }

    fun deleteAllIncomplete() {
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot delete tasks - Ditto not initialized")
                return@launch
            }

            // Use DQL DELETE to permanently delete all incomplete tasks
            dittoManager.executeQuery(
                "DELETE FROM tasks WHERE done = false",
                emptyMap()
            ).onSuccess { result ->
                Log.d(TAG, "Successfully deleted all incomplete tasks")
            }.onFailure { e ->
                Log.e(TAG, "Unable to delete incomplete tasks", e)
            }
        }
    }

    fun evictAllDeleted() {
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot evict tasks - Ditto not initialized")
                return@launch
            }

            dittoManager.executeQuery(
                "EVICT FROM tasks WHERE deleted = true",
                emptyMap()
            ).onSuccess {
                Log.d(TAG, "Successfully evicted all deleted tasks")
            }.onFailure { e ->
                Log.e(TAG, "Unable to evict deleted tasks", e)
            }
        }
    }
}

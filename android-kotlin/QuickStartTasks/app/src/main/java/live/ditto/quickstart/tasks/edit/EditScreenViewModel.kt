package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoManager
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

class EditScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "EditScreenViewModel"
    }

    private val dittoManager: DittoManager = TasksApplication.getDittoManager()
    private var _id: String? = null

    var title = MutableLiveData<String>("")
    var done = MutableLiveData<Boolean>(false)
    var canDelete = MutableLiveData<Boolean>(false)

    fun setupWithTask(id: String?) {
        canDelete.postValue(id != null)
        val taskId: String = id ?: return

        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot setup task - Ditto not initialized")
                return@launch
            }

            dittoManager.executeQuery(
                "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                mapOf("_id" to taskId)
            ).onSuccess { result ->
                val item = result.items.first()
                val task = Task.fromJson(item.jsonString())
                _id = task._id
                title.postValue(task.title)
                done.postValue(task.done)
            }.onFailure { e ->
                Log.e(TAG, "Unable to setup view task data", e)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot save task - Ditto not initialized")
                return@launch
            }

            if (_id == null) {
                // Add tasks into the ditto collection using DQL INSERT statement
                // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                dittoManager.executeQuery(
                    "INSERT INTO tasks DOCUMENTS (:doc)",
                    mapOf(
                        "doc" to mapOf(
                            "title" to title.value,
                            "done" to done.value,
                            "deleted" to false
                        )
                    )
                ).onFailure { e ->
                    Log.e(TAG, "Unable to save task", e)
                }
            } else {
                // Update tasks into the ditto collection using DQL UPDATE statement
                // https://docs.ditto.live/sdk/latest/crud/update#updating
                _id?.let { id ->
                    dittoManager.executeQuery(
                        """
                        UPDATE tasks
                        SET
                          title = :title,
                          done = :done
                        WHERE _id = :id
                        AND NOT deleted
                        """,
                        mapOf(
                            "title" to title.value,
                            "done" to done.value,
                            "id" to id
                        )
                    ).onFailure { e ->
                        Log.e(TAG, "Unable to save task", e)
                    }
                }
            }
        }
    }

    fun delete() {
        // UPDATE DQL Statement using Soft-Delete pattern
        // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
        viewModelScope.launch {
            if (!dittoManager.isDittoInitialized()) {
                Log.w(TAG, "Cannot delete task - Ditto not initialized")
                return@launch
            }

            _id?.let { id ->
                dittoManager.executeQuery(
                    "UPDATE tasks SET deleted = true WHERE _id = :id",
                    mapOf("id" to id)
                ).onFailure { e ->
                    Log.e(TAG, "Unable to set deleted=true", e)
                }
            }
        }
    }
}

package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context

class TasksApplication : Application() {

    companion object {
        private var instance: TasksApplication? = null
        private lateinit var dittoManager: DittoManager

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        fun getDittoManager(): DittoManager {
            return dittoManager
        }
    }

    init {
        instance = this
    }
    
    override fun onCreate() {
        super.onCreate()
        dittoManager = DittoManager(applicationContext)
    }
}

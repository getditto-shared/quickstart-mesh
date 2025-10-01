package live.ditto.quickstart.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import android.os.StrictMode

class MainActivity : ComponentActivity() {

    private lateinit var dittoManager: DittoManager
    private var isDittoInitialized = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // After permissions are granted/denied, refresh Ditto's permission state
        if (dittoManager.isDittoInitialized()) {
            dittoManager.requireDitto().refreshPermissions()
        }

        // Check if we still have missing permissions
        checkAndRequestPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog() // Log violations to logcat
                    .build()
            )
        }

        // Get DittoManager from Application
        dittoManager = TasksApplication.getDittoManager()

        // Initialize Ditto and handle permissions on app start
        lifecycleScope.launch {
            initializeDittoWithRetry()
        }

        setContent {
            Root(isDittoInitialized = isDittoInitialized.value)
        }
    }

    private suspend fun initializeDittoWithRetry() {
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries && !isDittoInitialized.value) {
            try {
                dittoManager.initDitto()
                isDittoInitialized.value = true
                Log.d("MainActivity", "Ditto initialized successfully")

                // Setup subscriptions immediately after init
                setupDittoSubscriptions()

                // Now that Ditto is initialized, check permissions
                checkAndRequestPermissions()

            } catch (e: Exception) {
                retryCount++
                Log.e("MainActivity", "Failed to initialize Ditto (attempt $retryCount/$maxRetries)", e)

                if (retryCount < maxRetries) {
                    // Wait before retrying
                    delay(1000L * retryCount) // Exponential backoff
                } else {
                    Log.e("MainActivity", "Failed to initialize Ditto after $maxRetries attempts")
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!isDittoInitialized.value) {
            Log.w("MainActivity", "Cannot check permissions - Ditto not initialized")
            return
        }

        try {
            val missingPermissions = dittoManager.missingPermissions()
            if (missingPermissions.isNotEmpty()) {
                Log.d("MainActivity", "Requesting permissions: ${missingPermissions.joinToString()}")
                requestPermissionLauncher.launch(missingPermissions)
            } else {
                Log.d("MainActivity", "All permissions granted")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions", e)
        }
    }

    private suspend fun setupDittoSubscriptions() {
        try {
            dittoManager.registerSubscription(TasksSubscription())
            Log.d("MainActivity", "Ditto subscriptions set up")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up subscriptions", e)
        }
    }
}




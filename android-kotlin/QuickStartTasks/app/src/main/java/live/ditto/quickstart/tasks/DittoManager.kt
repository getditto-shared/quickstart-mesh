package live.ditto.quickstart.tasks

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import live.ditto.Ditto
import live.ditto.DittoError
import live.ditto.DittoIdentity
import live.ditto.DittoLogger
import live.ditto.DittoLogLevel
import live.ditto.DittoQueryResult
import live.ditto.DittoSyncSubscription
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.transports.DittoSyncPermissions
import javax.inject.Singleton

@Singleton
class DittoManager(
    private val context: Context,
) {
    companion object {
        private const val TAG: String = "DittoManager"
    }

    private lateinit var ditto: Ditto
    private val currentSubscriptions: MutableMap<DittoCollectionSubscription, DittoSyncSubscription> = mutableMapOf()

    val syncScopes = mapOf(
        "local_mesh_only" to "SmallPeersOnly"
    )

    suspend fun initDitto() {
        try {
            DittoLogger.minimumLogLevel = DittoLogLevel.DEBUG

            val androidDependencies = DefaultAndroidDittoDependencies(context)

            val identity = DittoIdentity.OnlinePlayground(
                dependencies = androidDependencies,
                appId = BuildConfig.DITTO_APP_ID,
                token = BuildConfig.DITTO_PLAYGROUND_TOKEN,
                customAuthUrl = BuildConfig.DITTO_AUTH_URL,
                enableDittoCloudSync = false
            )

            ditto = Ditto(
                dependencies = androidDependencies,
                identity = identity
            ).apply {
                disableSyncWithV3()

                updateTransportConfig { transportConfig ->
                    //transportConfig.connect.websocketUrls.add(BuildConfig.DITTO_WEBSOCKET_URL)
                }

                store.execute(
                    "ALTER SYSTEM SET USER_COLLECTION_SYNC_SCOPES = :syncScopes",
                    mapOf("syncScopes" to syncScopes)
                )
                store.execute("ALTER SYSTEM SET rotating_log_file_max_size_mb = ${BuildConfig.DITTO_LOG_SIZE}")
                store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")
                startSync()
            }

        } catch (e: DittoError) {
            Log.e(TAG, e.message ?: "Failed to initialize Ditto")
            throw e
        }
    }

    fun requireDitto(): Ditto {
        if (::ditto.isInitialized) {
            return ditto
        } else {
            throw DittoNotInitializedException()
        }
    }

    fun isDittoInitialized(): Boolean {
        return ::ditto.isInitialized
    }

    fun missingPermissions(): Array<String> {
        return DittoSyncPermissions(context = context).missingPermissions()
    }

    suspend fun executeQuery(query: String, args: Map<String, Any?>?): Result<DittoQueryResult> {
        return try {
            if (::ditto.isInitialized) {
                Result.success(ditto.store.execute(query, args))
            } else {
                Result.failure(DittoNotInitializedException())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Query execution failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun liveQueryAsFlow(subscriptionQuery: String, args: Map<String, Any?>): Flow<DittoQueryResult> {
        return ditto.store.registerObserverAsFlow(
            query = subscriptionQuery,
            params = args
        )
    }

    fun registerSubscription(dittoCollectionSubscription: DittoCollectionSubscription) {
        val dittoSyncSubscription = ditto.sync.registerSubscription(
            query = dittoCollectionSubscription.subscriptionQuery,
            arguments = dittoCollectionSubscription.subscriptionQueryArgs
        )
        currentSubscriptions[dittoCollectionSubscription] = dittoSyncSubscription
    }

    fun startSync() {
        if (::ditto.isInitialized && !ditto.isSyncActive) {
            try {
                ditto.startSync()
            } catch (e: DittoError) {
                Log.e(TAG, "Unable to start sync", e)
                throw e
            }
        }
    }

    fun stopSync() {
        if (::ditto.isInitialized && ditto.isSyncActive) {
            try {
                ditto.stopSync()
            } catch (e: DittoError) {
                Log.e(TAG, "Unable to stop sync", e)
                throw e
            }
        }
    }

    fun isSyncActive(): Boolean {
        return if (::ditto.isInitialized) {
            ditto.isSyncActive
        } else {
            false
        }
    }

    fun toggleBluetoothLE() {
        if (::ditto.isInitialized) {
            ditto.updateTransportConfig { config ->
                config.peerToPeer.bluetoothLe.enabled = !config.peerToPeer.bluetoothLe.enabled
            }
        }
    }

    fun toggleLAN() {
        if (::ditto.isInitialized) {
            ditto.updateTransportConfig { config ->
                config.peerToPeer.lan.enabled = !config.peerToPeer.lan.enabled
            }
        }
    }

    fun toggleWifiAware() {
        if (::ditto.isInitialized) {
            ditto.updateTransportConfig { config ->
                config.peerToPeer.wifiAware.enabled = !config.peerToPeer.wifiAware.enabled
            }
        }
    }
}

class DittoNotInitializedException : IllegalStateException("Ditto has not been initialized. Call initDitto() first.")
package live.ditto.quickstart.tasks

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import live.ditto.DittoPresenceGraph
import live.ditto.DittoQueryResult
import live.ditto.DittoStore
import live.ditto.DittoStoreObserver
import live.ditto.Presence

/**
 * Builds a flow that emits changes from a managed [DittoStoreObserver].
 * @param bufferCapacity Buffer capacity
 */
fun DittoStore.registerObserverAsFlow(
    query: String,
    params: Map<String, Any?>,
    bufferCapacity: Int = Channel.UNLIMITED
): Flow<DittoQueryResult> = callbackFlow {
    val observer = registerObserver(query, params) { handler ->
        trySendBlocking(handler)
    }
    awaitClose { observer.close() }
}.buffer(bufferCapacity)

fun Presence.observeAsFlow(): Flow<DittoPresenceGraph> = callbackFlow {
    val observer = observe { presenceGraph ->
        trySend(presenceGraph)
    }
    awaitClose { observer.close() }
}
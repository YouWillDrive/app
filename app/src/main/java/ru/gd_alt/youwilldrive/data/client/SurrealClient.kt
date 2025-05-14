package ru.gd_alt.youwilldrive.data.client

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import ru.gd_alt.youwilldrive.data.cbor.SurrealCbor
import ru.gd_alt.youwilldrive.data.models.* // Assuming your data models: geometry, bounds, RecordID, TableName, LiveQueryUpdate
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.extensions.IExtension
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}


sealed class LiveQueryUpdate {
    abstract val id: String
    abstract val data: Map<String, Any?>

    /** A brand-new record appeared */
    data class Create(
        override val id: String,
        override val data: Map<String, Any?>
    ) : LiveQueryUpdate()

    /** An existing record changed */
    data class Update(
        override val id: String,
        override val data: Map<String, Any?>
    ) : LiveQueryUpdate()

    /** A record was removed */
    data class Delete(
        override val id: String,
        override val data: Map<String, Any?>
    ) : LiveQueryUpdate()

    /** Who the hell knows */
    data class Unknown(
        override val id: String,
        override val data: Map<String, Any?>,
        val action: String
    ) : LiveQueryUpdate()
}

class ConnectionError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class SurrealDBException(val code: Int, message: String) : RuntimeException("SurrealDB Error $code: $message")

// Data classes for RPC responses
data class RpcSuccessResponse(
    val id: Long,
    val result: Any?
)
data class RpcErrorDetails(
    val code: Int,
    val message: String
)
data class RpcErrorResponse(
    val id: Long,
    val error: RpcErrorDetails
)

// Sealed classes for incoming messages
sealed class IncomingMessage {
    data class RpcResponse(val id: Long, val payload: RpcResponsePayload) : IncomingMessage()
    data class LiveUpdate(val update: LiveQueryUpdate) : IncomingMessage()
    data class Unknown(val payload: Any?) : IncomingMessage()
}

sealed class RpcResponsePayload {
    data class Success(val result: Any?) : RpcResponsePayload()
    data class Error(val error: RpcErrorDetails) : RpcResponsePayload()
}

typealias SyncLiveUpdateCallback       = (LiveQueryUpdate) -> Unit
typealias AsyncLiveUpdateCallback  = suspend (LiveQueryUpdate) -> Unit

sealed class LiveUpdateCallback {
    data class Sync(val cb: SyncLiveUpdateCallback) : LiveUpdateCallback()
    data class Async(val cb: AsyncLiveUpdateCallback) : LiveUpdateCallback()
}

// Data class for live query updates
// Assuming LiveQueryUpdate is defined in data.models or here if not
// data class LiveQueryUpdate(val id: String, val action: String, val result: Any?)

/**
 * Asynchronous SurrealDB client with automatic reconnection support and on-connect callback.
 * Uses Kotlin Coroutines and Java-WebSocket.
 */
class SurrealDBClient private constructor(
    private val uri: URI,
    private val user: String,
    private val password: String,
    private val attemptReconnect: Boolean,
    private val onConnectCallback: suspend (SurrealDBClient) -> Unit
) {
    private var webSocketClient: WebSocketClient? = null
    private val nextId = AtomicLong(0)
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<RpcResponsePayload>>()
    private val liveQueries = ConcurrentHashMap<String, Triple<LiveUpdateCallback, String?, Map<String, Any?>?>>()
    private val liveQueriesMutex = Mutex()

    // Scope for handling messages; recreated on each connection
    private var clientScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Separate scope for scheduling reconnections
    private val reconnectionScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val protocols: List<IProtocol> = listOf(Protocol("cbor"))
    private var connectionReadyDeferred: CompletableDeferred<Unit> = CompletableDeferred()
    private val logger = KotlinLogging.logger {}

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    private var reconnectJob: Job? = null

    /**
     * Initialize or re-initialize the WebSocket client, perform signin, and invoke the on-connect callback.
     */
    private suspend fun initialize() {
        // Reset state
        connectionReadyDeferred = CompletableDeferred()
        clientScope.cancel() // Cancel any previous message handlers
        clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        nextId.set(0)
        pendingRequests.clear()

        // Create WebSocket client instance
        webSocketClient = object : WebSocketClient(uri, Draft_6455(emptyList<IExtension>(), protocols)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                logger.info { "SurrealDB WebSocket connected: ${handshakedata?.httpStatus} - ${handshakedata?.httpStatusMessage}" }

                clientScope.launch {
                    while (isActive) {
                        delay(5_000L)
                        webSocketClient?.sendPing()
                    }
                }

                // Update connection status
                _connectionStatus.value = ConnectionStatus.CONNECTING

                // Perform signin
                clientScope.launch {
                    val signinId = nextId.incrementAndGet()
                    val signinRequest = RpcRequest(
                        id = signinId,
                        method = "signin",
                        params = listOf(mapOf("user" to user, "pass" to password))
                    )
                    val signinDeferred = CompletableDeferred<RpcResponsePayload>()
                    pendingRequests[signinId] = signinDeferred

                    try {
                        send(SurrealCbor.cbor.encode(signinRequest))
                        when (val result = signinDeferred.await()) {
                            is RpcResponsePayload.Success -> {
                                logger.info { "SurrealDB signin successful" }
                                connectionReadyDeferred.complete(Unit)
                                _connectionStatus.value = ConnectionStatus.CONNECTED
                                reconnectJob = null
                            }
                            is RpcResponsePayload.Error -> {
                                val err = result.error
                                val msg = "SurrealDB signin failed: ${err.code} - ${err.message}"
                                logger.error { msg }
                                connectionReadyDeferred.completeExceptionally(ConnectionError(msg))
                                closeConnection(0, "")
                                _connectionStatus.value = ConnectionStatus.ERROR
                            }
                        }
                    } catch (e: Exception) {
                        val msg = "Exception during signin: ${e.message}"
                        logger.error { msg }
                        if (!signinDeferred.isCompleted) {
                            signinDeferred.completeExceptionally(ConnectionError(msg, e))
                        }
                        if (!connectionReadyDeferred.isCompleted) {
                            connectionReadyDeferred.completeExceptionally(ConnectionError(msg, e))
                        }
                        closeConnection(0, "")
                        _connectionStatus.value = ConnectionStatus.ERROR
                    } finally {
                        pendingRequests.remove(signinId)
                    }
                }
            }

            override fun onMessage(message: String?) {
                logger.error { "Received unexpected text message: $message" }
            }

            override fun onMessage(bytes: ByteBuffer?) {
                bytes?.let {
                    try {
                        val decoded: Any? = SurrealCbor.cbor.decode(it.array())
                        handleIncomingMessage(decoded)
                    } catch (e: Exception) {
                        logger.error { "Error decoding CBOR: ${e.message}" }
                        e.printStackTrace()
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                logger.info { "SurrealDB WebSocket closed: code=$code, reason='$reason', remote=$remote" }
                println("SurrealDB WebSocket closed: code=$code, reason='$reason', remote=$remote")
                val ex = ConnectionError("WebSocket closed: $code - $reason")
                pendingRequests.forEach { (_, d) -> d.completeExceptionally(ex) }

                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                var backoff = 1000L

                // Attempt reconnection if enabled
                if (attemptReconnect && (reconnectJob == null || reconnectJob!!.isCompleted)) {
                    reconnectJob = reconnectionScope.launch {
                        while (isActive) {
                            try {
                                initialize()
                                _connectionStatus.value = ConnectionStatus.CONNECTED
                                reconnectJob = null
                                break
                            } catch (t: Throwable) {
                                logger.warn { "Reconnect failed (${t.message}), will retry in ${backoff}ms" }
                                backoff = (backoff + backoff / 2).coerceAtMost(10_000L)
                                delay(backoff)
                            }
                        }
                    }
                }
                else {
                    if (!connectionReadyDeferred.isCompleted) {
                        connectionReadyDeferred.completeExceptionally(ex)
                    }
                    if (!attemptReconnect) runBlocking { liveQueriesMutex.withLock { liveQueries.clear() } }
                    clientScope.cancel()
                }
            }

            override fun onError(ex: Exception?) {
                logger.error { "SurrealDB WebSocket error: ${ex?.message}" }
                ex?.printStackTrace()
                val err = ConnectionError("WebSocket error", ex)
                pendingRequests.forEach { (_, d) -> d.completeExceptionally(err) }
                if (!connectionReadyDeferred.isCompleted) {
                    connectionReadyDeferred.completeExceptionally(err)
                }
            }
        }

        // Connect and await sign-in
        withContext(Dispatchers.IO) {
            val ok = webSocketClient?.connectBlocking(5, TimeUnit.SECONDS)
            ok?.let { if (!it) throw ConnectionError("Timed out connecting after 5s") }
        }
        connectionReadyDeferred.await()
        // Invoke user callback after successful (re)connection and sign-in
        onConnectCallback(this@SurrealDBClient)

        if (!liveQueries.isEmpty()) {
            logger.info { "Restarting live queries..." }
            restartLiveQueries()
        }
    }

    /** Restart all the live queries and recalculate their IDs. */
    private suspend fun restartLiveQueries() {
        liveQueriesMutex.withLock {
            for ((id, triple) in liveQueries) {
                try {
                    val (callback, reproduceVia, reproduceVars) = triple
                    if (reproduceVia == null) {
                        logger.info { "Live query with id $id is not reproduceable. May be intended." }
                        liveQueries.remove(id)
                        continue
                    }
                    val result = sendRpc("query", listOf(reproduceVia, reproduceVars ?: emptyMap<String, Any?>()))
                    val newUuid = (result as List<Map<String, Any?>?>)[0]?.get("result")
                    liveQueries[newUuid.toString()] = Triple(callback, reproduceVia, reproduceVars)

                    logger.info { "Restarted live query: $id -> ${newUuid.toString()}" }
                    liveQueries.remove(id)
                } catch (e: Exception) {
                    logger.error { "Error restarting live query: ${e.message}" }
                }
            }
        }
    }

    /** Handle incoming CBOR-decoded messages. */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleIncomingMessage(decoded: Any?) {
        if (decoded is Map<*, *>) {
            val id = decoded["id"]
            val result = decoded["result"]
            val error = decoded["error"]

            when {
                id != null && id is Number -> {
                    val mid = id.toLong()
                    val deferred = pendingRequests.remove(mid)
                    if (deferred != null) {
                        when (error) {
                            null -> deferred.complete(RpcResponsePayload.Success(result))
                            is Map<*, *> -> {
                                val code = (error["code"] as? Number)?.toInt() ?: -1
                                val msg = error["message"] as? String ?: "Unknown"
                                deferred.complete(RpcResponsePayload.Error(RpcErrorDetails(code, msg)))
                            }

                            else -> deferred.completeExceptionally(RuntimeException("Unexpected response: $decoded"))
                        }
                    } else {
                        logger.error { "Unknown request id: $mid" }
                    }
                }
                id == null && result is Map<*, *> -> {
                    val qid = result["id"] as? Uuid
                    val action = result["action"] as? String
                    val data = result["result"]
                    if (qid != null && action != null) {
                        var update = when (action) {
                            "CREATE" -> LiveQueryUpdate.Create(qid.toString(), data as Map<String, Any?>)
                            "UPDATE" -> LiveQueryUpdate.Update(qid.toString(), data as Map<String, Any?>)
                            "DELETE" -> LiveQueryUpdate.Delete(qid.toString(), data as Map<String, Any?>)
                            else -> LiveQueryUpdate.Unknown(qid.toString(), data as Map<String, Any?>, action)
                        }
                        clientScope.launch {
                            liveQueriesMutex.withLock {
                                val (cb, _, _) = liveQueries[qid.toString()] ?: return@withLock
                                try {
                                    when (cb) {
                                        is LiveUpdateCallback.Sync -> cb.cb(update)
                                        is LiveUpdateCallback.Async -> cb.cb(update)
                                    }
                                } catch (e: Exception) {
                                    logger.error { "Callback error: ${e.message}" }
                                }
                            }
                        }
                    } else {
                        logger.error { "Unexpected live update structure: $decoded" }
                    }
                }
                else -> logger.info { "Unhandled message: $decoded" }
            }
        } else {
            logger.error { "Non-map message: $decoded" }
        }
    }

    // Core RPC sending
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun sendRpcInternal(method: String, params: List<Any?> = emptyList()): RpcResponsePayload {
        ensureConnectedAndReady()
        val id = nextId.incrementAndGet()
        val req = RpcRequest(id, method, params)
        val deferred = CompletableDeferred<RpcResponsePayload>()
        pendingRequests[id] = deferred
        try {
            val bytes = SurrealCbor.cbor.encode(req)
            Log.d("SurrealDBClient", "Sending RPC: $req")
            Log.d("SurrealDBClient", "Encoded bytes: ${bytes.toHexString()}")
            webSocketClient?.send(bytes)
        } catch (e: Exception) {
            pendingRequests.remove(id)
            val err = RuntimeException("Failed to send '$method'", e)
            deferred.completeExceptionally(err)
            throw err
        }
        return deferred.await()
    }

    private suspend fun ensureConnectedAndReady() {
        connectionReadyDeferred.await()
        if (webSocketClient?.isOpen != true) throw ConnectionError("WebSocket not open")
    }

    private suspend fun sendRpc(method: String, params: List<Any?> = emptyList()): Any? {
        return when (val payload = sendRpcInternal(method, params)) {
            is RpcResponsePayload.Success -> payload.result
            is RpcResponsePayload.Error -> throw SurrealDBException(payload.error.code, payload.error.message)
        }
    }

    /**
     * Assign namespace and database.
     * @param ns namespace
     * @param db database
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun use(ns: String, db: String) {
        sendRpc("use", listOf(ns, db))
    }

    /**
     * Get current authenticated user info.
     * @return user info payload.
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun info(): Any? {
        return sendRpc("info")
    }

    /**
     * Get SurrealDB version details.
     * @return SurrealDB version map (version, build, timestamp).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun version(): Any? {
        return sendRpc("version")
    }

    /**
     * Set a key-value pair for the current connection. Not persisted between connections.
     * @param key key.
     * @param value value (must be CBOR encodable).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun let(key: String, value: Any?) {
        // Value must be encodable by CBOR via your customEnHook
        sendRpc("let", listOf(key, value))
    }

    /**
     * Unset a key-value pair.
     * @param key key.
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun unset(key: String) {
        sendRpc("unset", listOf(key))
    }

    /**
     * Run a query with optional variables.
     * @param query query string.
     * @param variables query variables as a Map, or null.
     * @return query result (typically a list of results, one for each statement).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun query(query: String, variables: Map<String, Any?>? = null): Any? {
        return sendRpc("query", listOf(query, variables ?: emptyMap<String, Any?>()))
    }

    /**
     * Run a defined function with arguments.
     * Machine learning functions are available in the 'ml' namespace, custom functions in 'fn'.
     * @param function function name (e.g., "ml::analyze_sentiment").
     * @param args function arguments.
     * @param version function version (optional).
     * @return function result.
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun run(function: String, vararg args: Any?, version: String? = null): Any? {
        val params = mutableListOf<Any?>(function, version ?: "")
        params.addAll(args)
        return sendRpc("run", params)
    }


    /**
     * Select an entity by ID (table:id) or all entities in a table (table).
     * @param entity entity identifier (table or table:id).
     * @return list of selected entities.
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun select(entity: String): List<*>? {
        // Assuming select always returns a list on success
        return sendRpc("select", listOf(entity)) as? List<*>
    }

    /**
     * Insert a new entity into a table.
     * @param table table name.
     * @param data entity data as a Map or data class (must be CBOR encodable via customEnHook).
     * @return inserted entity (typically a list containing the inserted record).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun insert(table: String, data: Map<String, Any?>): List<*>? {
        return sendRpc("insert", listOf(table, data)) as? List<*>
    }

    /**
     * Relate two entities using a relation table.
     * Argument order: relation, in_entity, out_entity.
     * @param relation relation table name.
     * @param inEntity incoming entity ID (table:id).
     * @param outEntity outgoing entity ID (table:id).
     * @param data additional data for the relation as a Map (must be CBOR encodable).
     * @return inserted relation (typically a list containing the relation record).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun relation(relation: String, inEntity: String, outEntity: String, data: Map<String, Any?>? = null): List<*>? {
        val relationData = mutableMapOf<String, Any?>("in" to inEntity, "out" to outEntity)
        data?.let { relationData.putAll(it) }
        return sendRpc("insert_relation", listOf(relation, relationData)) as? List<*>
    }

    /**
     * Relate two entities using a relation table.
     * Argument order: in_entity, relation, out_entity. Matches SurrealQL RELATE syntax.
     * @param inEntity incoming entity ID (table:id).
     * @param relation relation table name.
     * @param outEntity outgoing entity ID (table:id).
     * @param data additional data for the relation as a Map (must be CBOR encodable).
     * @return inserted relation (typically a list containing the relation record).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun relate(inEntity: String, relation: String, outEntity: String, data: Map<String, Any?>? = null): List<*>? {
        // The relate RPC method params are [in, relation, out, data]
        return sendRpc("relate", listOf(inEntity, relation, outEntity, data ?: emptyMap<String, Any?>())) as? List<*>
    }


    /**
     * Update an entity. Replaces the entire record(s).
     * Use table:id for a single record, table for all records in a table.
     * @param entity entity identifier (table or table:id).
     * @param data entity data as a Map or data class (must be CBOR encodable via customEnHook).
     * @return updated entity (list containing the updated record(s)).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun update(entity: String, data: Map<String, Any?>): List<*>? {
        return sendRpc("update", listOf(entity, data)) as? List<*>
    }

    /**
     * Update or insert an entity if it doesn't exist (UPSERT).
     * Use table:id for a single record, table for all records in a table.
     * @param entity entity identifier (table or table:id).
     * @param data entity data as a Map or data class (must be CBOR encodable via customEnHook).
     * @return updated/inserted entity (list containing the record(s)).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun upsert(entity: String, data: Map<String, Any?>): List<*>? {
        return sendRpc("upsert", listOf(entity, data)) as? List<*>
    }

    /**
     * Merge data into an existing entity or entities.
     * Use table:id for a single record, table for all records in a table.
     * @param entity entity identifier (table or table:id).
     * @param data data to merge as a Map (must be CBOR encodable).
     * @return merged entity (list containing the record(s)).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun merge(entity: String, data: Map<String, Any?>): List<*>? {
        // Data must be a Map and encodable by CBOR.
        return sendRpc("merge", listOf(entity, data)) as? List<*>
    }

    /**
     * Apply a JSON patch to an entity or entities.
     * Use table:id for a single record, table for all records in a table.
     * @param entity entity identifier (table or table:id).
     * @param patch JSON patch as a List of Map operations (must be CBOR encodable).
     * @return patched entity (list containing the record(s)).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun patch(entity: String, patch: List<Map<String, Any?>>): List<*>? {
        // Patch must be a List of Maps representing JSON Patch operations.
        return sendRpc("patch", listOf(entity, patch)) as? List<*>
    }


    /**
     * Delete an entity or entities.
     * Use table:id for a single record, table for all records in a table.
     * @param entity entity identifier (table or table:id).
     * @return deleted entity (list containing the result, often empty list of deleted records).
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     */
    suspend fun delete(entity: String): List<*>? {
        return sendRpc("delete", listOf(entity)) as? List<*>
    }

    /**
     * Start a live query on a specific table (entity).
     * The server returns a live query ID. Use [attachLiveQuery] if starting a custom LIVE query via [query].
     * You must kill the live query with [kill] when done.
     * @param entity table name to watch for changes.
     * @param callback callback function to execute when query results change. Must be an instance of LiveUpdateCallback.
     * @param doNotReproduce if true, the query will not be reproduced on reconnection. Recommended to be set to true if you start your query in onConnectCallback.
     * @return live query UUID string.
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     * @throws RuntimeException if the response from the server is not the expected string ID.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun live(entity: String, callback: LiveUpdateCallback, doNotReproduce: Boolean = false): String {
        val responseResult = sendRpc("live", listOf(entity))

        if (responseResult is Uuid) {
            attachLiveQuery(responseResult.toString(), callback, "LIVE SELECT FROM $entity", doNotReproduce = doNotReproduce)
            return responseResult.toString()
        } else {
            throw RuntimeException("Failed to start live query: Expected UUID but received $responseResult")
        }
    }

    /**
     * Attach a callback function to a live query ID that was previously started
     * (e.g., by executing a `LIVE SELECT ...` query via the [query] method).
     * @param queryId live query ID obtained from the server response.
     * @param callback callback function to execute when query results change. Must be an instance of LiveUpdateCallback.
     * @param reproduceVia query string used to reproduce the live query. Will be used to recreate queries on reconnection.
     * @param reproduceVars optional variables used in the query.
     * @param doNotReproduce if true, the query will not be reproduced on reconnection. Recommended to be set to true if you start your query in onConnectCallback.
     */
    suspend fun attachLiveQuery(queryId: String, callback: LiveUpdateCallback, reproduceVia: String, reproduceVars: Map<String, Any?>? = null, doNotReproduce: Boolean = false) {
        liveQueriesMutex.withLock {
            liveQueries[queryId] = Triple(callback, if (doNotReproduce) null else reproduceVia, reproduceVars ?: emptyMap())
            logger.info { "Attached callback for live query ID: $queryId" }
        }
    }

    /**
     * Run a custom query, get corresponding live query ID, and attach a callback.
     * Shorthand for combination of [query] and [attachLiveQuery].
     * @param query custom query string. Must include the `LIVE` keyword. E.g. `"LIVE SELECT * FROM table"`.
     * @param variables query variables as a Map, or null.
     * @param callback callback function to execute when query results change. Must be an instance of LiveUpdateCallback.
     * @param doNotReproduce if true, the query will not be reproduced on reconnection. Recommended to be set to true if you start your query in onConnectCallback.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun liveQuery(query: String, variables: Map<String, Any?>? = null, callback: LiveUpdateCallback, doNotReproduce: Boolean = false): String {
        val responseResult = sendRpc("query", listOf(query, variables ?: emptyMap<String, Any?>()))

        if (responseResult is List<*>) {
            val result = responseResult.firstOrNull()
            if (result is Map<*, *>) {
                val qid = result["result"] as? Uuid
                if (qid != null) {
                    attachLiveQuery(qid.toString(), callback, query, variables, doNotReproduce)
                    return qid.toString()
                } else {
                    throw RuntimeException("Failed to start live query: Expected UUID but received $result")
                }
            } else {
                throw RuntimeException("Failed to start live query: Expected Map but received $result")
            }
        }

        if (responseResult is Uuid) {
            attachLiveQuery(responseResult.toString(), callback, query, variables)
            return responseResult.toString()
        } else {
            throw RuntimeException("Failed to start live query: Expected UUID but received $responseResult")
        }
    }

    /**
     * Detach a callback function from a live query ID.
     * Note: This only stops receiving updates in this client instance. It does NOT kill the query on the server.
     * Use the [kill] method to stop the query on the server.
     * @param queryId live query ID.
     */
    suspend fun detachLiveQuery(queryId: String) {
        liveQueriesMutex.withLock {
            if (liveQueries.remove(queryId) != null) {
                logger.info { "Detached callback for live query ID: $queryId" }
            } else {
                logger.error { "No callback found to detach for live query ID: $queryId" }
            }
        }
    }

    /**
     * Kill a live query on the SurrealDB server and detach its callback in the client.
     * This stops the server from sending updates for this query ID.
     * @param queryId live query ID.
     * @throws SurrealDBException if SurrealDB returns an error (though kill typically succeeds silently).
     * @throws ConnectionError if the connection fails.
     */
    suspend fun kill(queryId: String) {
        try {
            sendRpc("kill", listOf(queryId)) // Send kill command to the server
            logger.info { "Sent kill command for live query ID: $queryId" }
        } finally {
            // Always remove the callback locally, even if RPC failed, as the query state is uncertain
            liveQueriesMutex.withLock {
                if (liveQueries.remove(queryId) != null) {
                    logger.info { "Detached callback locally after killing query: $queryId" }
                }
            }
        }
    }

    /**
     * Gracefully close the client and all resources.
     */
    fun close() {
        logger.info { "Closing SurrealDB client..." }
        clientScope.cancel()
        webSocketClient?.takeIf { !it.isClosing && !it.isClosed }?.close()
        webSocketClient = null
        pendingRequests.clear()
        logger.info { "Client closed." }
    }

    companion object {
        /**
         * Factory to create a new client instance with optional reconnection and callback.
         */
        suspend fun create(
            endpoint: String,
            user: String,
            password: String,
            attemptReconnect: Boolean = false,
            onConnectCallback: suspend (SurrealDBClient) -> Unit = {}
        ): SurrealDBClient {
            if (!endpoint.startsWith("ws://") && !endpoint.startsWith("wss://")) {
                throw IllegalArgumentException("Endpoint must start with ws:// or wss://")
            }
            val uri = URI(if (endpoint.endsWith("/rpc")) endpoint else "${endpoint.removeSuffix("/")}/rpc")
            val client = SurrealDBClient(uri, user, password, attemptReconnect, onConnectCallback)
            client.initialize()
            return client
        }

        /**
         * Create client from environment variables and set namespace/db.
         */
        suspend fun fromEnv(
            attemptReconnect: Boolean = false,
            onConnectCallback: suspend (SurrealDBClient) -> Unit = {}
        ): SurrealDBClient {
            val endpoint = System.getenv("SURREAL_ENDPOINT") ?: error("SURREAL_ENDPOINT not set")
            val user = System.getenv("SURREAL_USER") ?: error("SURREAL_USER not set")
            val password = System.getenv("SURREAL_PASSWORD") ?: error("SURREAL_PASSWORD not set")
            val ns = System.getenv("SURREAL_NS") ?: error("SURREAL_NS not set")
            val db = System.getenv("SURREAL_DB") ?: error("SURREAL_DB not set")
            val client = create(endpoint, user, password, attemptReconnect, onConnectCallback)
            client.use(ns, db)
            return client
        }
    }
}

fun main() {
    runBlocking {
        try {
            val client = SurrealDBClient.create(
                "ws://87.242.117.89:5457/rpc",
                "root",
                "iwilldrive",
                attemptReconnect = true
            ) { cli ->
                println("Connected (or reconnected) to SurrealDB: ${cli.version()}")
            }
            println("Initial version: ${client.version()}")
            client.close()
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}

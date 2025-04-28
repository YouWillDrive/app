package ru.gd_alt.youwilldrive.data.client

import androidx.compose.runtime.State
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import ru.gd_alt.youwilldrive.data.cbor.SurrealCbor
import ru.gd_alt.youwilldrive.data.models.*
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.get
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.extensions.IExtension
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean


private abstract class CallbackType<T>
private class SuspendCallbackType : CallbackType<suspend (LiveQueryUpdate) -> Unit>()
private class RegularCallbackType : CallbackType<(LiveQueryUpdate) -> Unit>()

// Data class to represent a successful RPC response payload
data class RpcSuccessResponse(
    val id: Long,
    val result: Any?
)

// Data class to represent an error payload in an RPC response
data class RpcErrorDetails(
    val code: Int,
    val message: String
)

// Data class to represent an erroneous RPC response payload
data class RpcErrorResponse(
    val id: Long,
    val error: RpcErrorDetails
)

// Sealed class to represent the different types of messages received from SurrealDB
sealed class IncomingMessage {
    // Standard RPC response that matches a sent request ID
    data class RpcResponse(val id: Long, val payload: RpcResponsePayload) : IncomingMessage()
    // Live query update message (identified by null ID at top level)
    data class LiveUpdate(val update: LiveQueryUpdate) : IncomingMessage() // Assuming LiveQueryUpdate is a data class in ru.gd_alt.youwilldrive.data.models
    // Unhandled message structure
    data class Unknown(val payload: Any?) : IncomingMessage()
}

// Sealed class to represent the payload of a standard RPC response (either success or error)
sealed class RpcResponsePayload {
    data class Success(val result: Any?) : RpcResponsePayload()
    data class Error(val error: RpcErrorDetails) : RpcResponsePayload()
}

enum class Status {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    CONNECTING,
    READY,
    FAILED
}

/**
 * SurrealDB asynchronous client implemented in Kotlin with Java-WebSocket and CBOR.
 * Uses Kotlin Coroutines for asynchronous operations.
 */
class SurrealDBClient(
    endpoint: String,
    private val user: String,
    private val password: String,
    private val onConnectCallback: suspend (SurrealDBClient) -> Unit = {},
) {
    private var webSocketClient: WebSocketClient? = null
    private val nextId = AtomicLong(0) // Atomic counter for request IDs
    // Map to hold CompletableDeferred for pending requests, keyed by request ID
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<RpcResponsePayload>>()
    // Map to hold live query callbacks, keyed by live query ID
    // Pair<CallbackFunction, Boolean isAsync> - isAsync is ignored in this coroutine implementation
    private val liveQueries = ConcurrentHashMap<String, Triple<Any, Boolean, CallbackType<*>>>()
    private val liveQueriesMutex = Mutex() // Mutex to protect access to liveQueries map

    private val logger = KotlinLogging.logger {}

    // Coroutine scope tied to the client's lifecycle for handling background tasks like message receiving and callback dispatching
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // List of protocols supported by the WebSocket client
    private val protocols: List<IProtocol> = mutableListOf<IProtocol>(Protocol("cbor"))

    // Deferred to signal when the initial connection and signin are complete
    private var connectionReadyDeferred = CompletableDeferred<Unit>()

    // State of the connection
    private val _connectionState = MutableStateFlow(Status.DISCONNECTED)
    val connectionState: StateFlow<Status> = _connectionState
    val attemptReconnect = AtomicBoolean(false)
    val backoff = AtomicLong(0)

    // Data to save between connections
    val uri : URI
    val draft = Draft_6455(emptyList<IExtension>(), protocols)

    init {
        // Validate endpoint format
        if (!endpoint.startsWith("ws://") && !endpoint.startsWith("wss://")) {
            throw IllegalArgumentException("Endpoint must start with ws:// or wss://.")
        }

        // Ensure endpoint ends with /rpc
        uri = URI(if (endpoint.endsWith("/rpc")) endpoint else "$endpoint/rpc")
        handleConnection()
    }

    fun handleConnection() {
        // Initialize the WebSocket client with callbacks
        webSocketClient = object : WebSocketClient(uri, draft) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                logger.info {"SurrealDB WebSocket connected: ${handshakedata?.httpStatus} - ${handshakedata?.httpStatusMessage}"}
                _connectionState.value = Status.CONNECTED
                // Perform signin immediately upon successful connection
                clientScope.launch {
                    if (backoff.get() < 30000L) {
                        backoff.addAndGet(100L)
                    }
                    val signinId = nextId.incrementAndGet()
                    val signinRequest = RpcRequest(id = signinId, method = "signin", params = listOf(mapOf("user" to user, "pass" to password)))
                    val signinDeferred = CompletableDeferred<RpcResponsePayload>()
                    pendingRequests[signinId] = signinDeferred // Add deferred for signin response

                    try {
                        val cborMessage = SurrealCbor.cbor.encode(signinRequest)
                        send(cborMessage) // Use the client's send method

                        // Wait for the signin response
                        when (val signInResult = signinDeferred.await()) { // Await ONLY the signin deferred
                            is RpcResponsePayload.Success -> {
                                logger.info { "SurrealDB signin successful" }
                                connectionReadyDeferred.complete(Unit) // Signal that the client is ready for use
                                // Call the onConnect callback
                                try {
                                    onConnectCallback(this@SurrealDBClient)
                                } catch (e: Exception) {
                                    logger.error { "Error in onConnect callback: ${e.message}" }
                                    connectionReadyDeferred.completeExceptionally(e) // Signal failure
                                    closeConnection(0, "") // Close the connection on error
                                    _connectionState.value = Status.FAILED
                                }
                                _connectionState.value = Status.READY
                                backoff.set(0L) // Reset backoff on successful signin
                            }
                            is RpcResponsePayload.Error -> {
                                val error = signInResult.error
                                val errorMessage = "SurrealDB signin failed: ${error.code} - ${error.message}"
                                logger.error { errorMessage }
                                connectionReadyDeferred.completeExceptionally(ConnectionError(errorMessage)) // Signal failure
                                closeConnection(0, "") // Close the connection on authentication failure
                                _connectionState.value = Status.FAILED
                                delay(backoff.get())
                            }
                        }
                    } catch (e: Exception) {
                        val errorMessage = "Exception during SurrealDB signin or sending signin: ${e.message}"
                        logger.error { errorMessage }
                        // If the deferred wasn't completed by an error response, complete exceptionally here
                        if (!signinDeferred.isCompleted) {
                            signinDeferred.completeExceptionally(ConnectionError(errorMessage, e))
                        }
                        // Always complete the main connection deferred exceptionally if signin process failed
                        if (!connectionReadyDeferred.isCompleted) {
                            connectionReadyDeferred.completeExceptionally(ConnectionError(errorMessage, e))
                        }
                        closeConnection(0, "") // Close on exception
                        _connectionState.value = Status.FAILED
                        delay(backoff.get())
                    } finally {
                        pendingRequests.remove(signinId) // Clean up the signin deferred regardless of outcome
                    }
                }
            }

            override fun onMessage(message: String?) {
                // SurrealDB primarily uses CBOR for RPC, text messages are unexpected for standard RPC
                logger.error { "Received unexpected text message: $message" }
            }

            override fun onMessage(bytes: ByteBuffer?) {
                bytes?.let {
                    try {
                        // Decode the incoming CBOR message using the provided library
                        val decodedMessage: Any? = SurrealCbor.cbor.decode(it.array())
                        handleIncomingMessage(decodedMessage) // Process the decoded message
                    } catch (e: Exception) {
                        logger.error { "Error decoding incoming CBOR message: ${e.message}" }
                        e.printStackTrace()
                        // Depending on error severity, might need to close connection
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                logger.info { "SurrealDB WebSocket closed: code=$code, reason='$reason', remote=$remote" }
                val disconnectException = ConnectionError("WebSocket closed: $code - $reason")

                // Complete any pending requests with an exception indicating connection loss
                pendingRequests.forEach { (_, deferred) ->
                    deferred.completeExceptionally(disconnectException)
                }
                pendingRequests.clear()

                // If connection wasn't ready yet, complete the deferred exceptionally
                if (!connectionReadyDeferred.isCompleted) {
                    connectionReadyDeferred.completeExceptionally(disconnectException)
                }

                // Clear all live queries as the connection is broken
                clientScope.launch {
                    liveQueriesMutex.withLock {
                        liveQueries.clear()
                    }
                }

                // Cancel the client's coroutine scope
                clientScope.cancel()
                connectionReadyDeferred = CompletableDeferred<Unit>() // Reset the deferred for next connection attempt

                if (attemptReconnect.get()) {
                    logger.info { "Attempting to reconnect..." }
                    _connectionState.value = Status.RECONNECTING
                    handleConnection()
                } else {
                    _connectionState.value = Status.DISCONNECTED
                }
            }

            override fun onError(ex: Exception?) {
                logger.error { "SurrealDB WebSocket error: ${ex?.message}" }
                ex?.printStackTrace()

                // Complete any pending requests and connection deferred with the error
                val error = ConnectionError("WebSocket error", ex)
                pendingRequests.forEach { (_, deferred) ->
                    deferred.completeExceptionally(error)
                }
                if (!connectionReadyDeferred.isCompleted) {
                    connectionReadyDeferred.completeExceptionally(error)
                }

                if (attemptReconnect.get()) {
                    logger.info { "Attempting to reconnect..." }
                    _connectionState.value = Status.RECONNECTING
                    handleConnection()
                } else {
                    _connectionState.value = Status.DISCONNECTED
                }
            }
        }

        // Start the WebSocket connection attempt
        try {
            // connectBlocking() will block until the connection is established or fails.
            // This aligns better with the constructor finishing only when the connection state is determined.
            // Sign-in is handled async in onOpen.
            webSocketClient?.connectBlocking()
            _connectionState.value = Status.CONNECTING
        } catch (e: Exception) {
            val message = "Failed to establish SurrealDB WebSocket connection"
            logger.error { "$message: ${e.message}" }
            // If connectBlocking fails, the connectionReadyDeferred is completed exceptionally here
            if (!connectionReadyDeferred.isCompleted) {
                connectionReadyDeferred.completeExceptionally(ConnectionError(message, e))
                _connectionState.value = Status.FAILED
            }
            // Re-throw the exception from the constructor as connection failed
            throw ConnectionError(message, e)
        }
    }

    /**
     * Handles incoming decoded messages from the WebSocket.
     * Distinguishes between standard RPC responses and live query updates.
     */
    private fun handleIncomingMessage(decodedMessage: Any?) {
        // Based on SurrealDB RPC protocol over WebSocket:
        // Standard response: { "id": <id>, "result": <result> } OR { "id": <id>, "error": <error> }
        // Live update: { "id": null, "result": { "id": <query_id>, "action": <action>, "result": <data> } }

        if (decodedMessage is Map<*, *>) {
            val id = decodedMessage["id"]
            val result = decodedMessage["result"]
            val error = decodedMessage["error"]

            when {
                // Case 1: Standard RPC response with a non-null ID
                id != null && id is Number -> {
                    val messageId = id.toLong() // Convert to Long
                    val deferred = pendingRequests.remove(messageId) // Get and remove the pending deferred

                    if (deferred != null) {
                        when {
                            error == null -> {
                                // Success response
                                deferred.complete(RpcResponsePayload.Success(result))
                            }
                            error is Map<*, *> -> {
                                // Error response
                                try {
                                    val errorCode = (error["code"] as? Number)?.toInt() ?: -1
                                    val errorMessage = error["message"] as? String ?: "Unknown error"
                                    deferred.complete(RpcResponsePayload.Error(RpcErrorDetails(errorCode, errorMessage)))
                                } catch (e: Exception) {
                                    logger.error { "Error parsing RPC error response for id $messageId: ${e.message}" }
                                    deferred.completeExceptionally(RuntimeException("Error parsing RPC error response", e))
                                }
                            }
                            else -> {
                                // Unexpected response structure for a known ID
                                logger.error { "Received unexpected RPC response structure for id $messageId: $decodedMessage" }
                                deferred.completeExceptionally(RuntimeException("Received unexpected RPC response structure: $decodedMessage"))
                            }
                        }
                    } else {
                        // Response for an unknown or already handled request ID
                        logger.error { "Received response for unknown request id: $messageId" }
                    }
                }
                // Case 2: Live query update message (null ID at the top level)
                id == null && result is Map<*, *> -> {
                    try {
                        // Parse the inner structure of the live update
                        val liveQueryId = result["id"] as? String // The actual live query ID string
                        val liveUpdateAction = result["action"] as? String
                        val liveUpdateResultData = result["result"] // The data payload of the update

                        if (liveQueryId != null && liveUpdateAction != null) {
                            // Create the LiveQueryUpdate data class instance
                            val liveQueryUpdate = LiveQueryUpdate(liveQueryId, liveUpdateAction, liveUpdateResultData)

                            // Dispatch the update to the appropriate callback asynchronously
                            clientScope.launch {
                                liveQueriesMutex.withLock {
                                    val (callback, _, callbackType) = liveQueries[liveQueryId] ?: return@withLock

                                    try {
                                        when (callbackType) {
                                            is SuspendCallbackType -> {
                                                // Cast und Aufruf der Suspend-Funktion
                                                @Suppress("UNCHECKED_CAST")
                                                val suspendCallback = callback as suspend (LiveQueryUpdate) -> Unit
                                                suspendCallback(liveQueryUpdate)
                                            }
                                            is RegularCallbackType -> {
                                                // Cast und Aufruf der regulären Funktion
                                                @Suppress("UNCHECKED_CAST")
                                                val regularCallback = callback as (LiveQueryUpdate) -> Unit
                                                regularCallback(liveQueryUpdate)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        logger.error { "Error executing callback for live query $liveQueryId: ${e.message}" }
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } else {
                            // Null ID message with unexpected structure within 'result'
                            logger.error { "Received null-id message with unexpected 'result' structure: $decodedMessage" }
                        }

                    } catch (e: Exception) {
                        // Error processing the live query update structure
                        logger.error { "Error processing live query update message: ${e.message}" }
                        e.printStackTrace()
                    }
                }
                // Case 3: Any other unhandled message structure
                else -> {
                    logger.info { "Received unhandled message structure: $decodedMessage" }
                }
            }
        } else {
            // Received a message that wasn't a Map
            logger.error { "Received unexpected non-map message: $decodedMessage" }
        }
    }

    /**
     * Sends an RPC request message to SurrealDB and awaits the response.
     * This is the core internal method for all RPC calls.
     *
     * @param method The SurrealDB RPC method name (e.g., "signin", "use", "query").
     * @param params The parameters for the method, as a list.
     * @return The parsed result payload from the server response.
     * @throws ConnectionError if the client is not connected or connection is lost.
     * @throws RuntimeException if sending fails or an unexpected response is received.
     */
    private suspend fun sendRpcInternal(method: String, params: List<Any?> = emptyList()): RpcResponsePayload {
        // Ensure connection is open and signin is complete before sending
        ensureConnectedAndReady()

        val currentId = nextId.incrementAndGet() // Generate a new unique ID for the request
        val request = RpcRequest(id = currentId, method = method, params = params)

        // Create a deferred result for this request ID
        val deferred = CompletableDeferred<RpcResponsePayload>()
        pendingRequests[currentId] = deferred // Store the deferred result

        try {
            // Encode the request to CBOR and send it over the WebSocket
            val cborMessage = SurrealCbor.cbor.encode(request)
            webSocketClient?.send(cborMessage)
        } catch (e: Exception) {
            // If sending fails, remove the pending request and complete the deferred exceptionally
            pendingRequests.remove(currentId)
            val sendError = RuntimeException("Failed to send RPC message for method '$method'", e)
            deferred.completeExceptionally(sendError)
            throw sendError // Re-throw the exception
        }

        // Await the completion of the deferred result (handled by handleIncomingMessage)
        return deferred.await()
    }

    /**
     * Suspends until the WebSocket connection is open and the initial signin is complete.
     * @throws ConnectionError if connection or signin fails.
     */
    private suspend fun ensureConnectedAndReady() {
        // Await the completion of the connectionReadyDeferred.
        // If connection or signin failed, await() will throw the exception it was completed with.
        connectionReadyDeferred.await()

        // Additional check to ensure the client is still considered open and not closing
        if (webSocketClient?.isOpen != true) {
            throw ConnectionError("WebSocket is not open.")
        }
    }

    /**
     * Sends an RPC request and processes the result, throwing a structured exception on error.
     * This is the method called by the public API functions.
     *
     * @param method The SurrealDB RPC method name.
     * @param params The parameters for the method.
     * @return The result payload from the server on success.
     * @throws SurrealDBException if SurrealDB returns an error code in the response.
     * @throws ConnectionError if the connection fails before or during the request.
     * @throws RuntimeException for other client-side errors (e.g., sending/decoding).
     */
    private suspend fun sendRpc(method: String, params: List<Any?> = emptyList()): Any? {
        val responsePayload = sendRpcInternal(method, params)
        return when (responsePayload) {
            is RpcResponsePayload.Success -> responsePayload.result
            is RpcResponsePayload.Error -> {
                // Throw a specific exception for SurrealDB server errors
                throw SurrealDBException(responsePayload.error.code, responsePayload.error.message)
            }
        }
    }

    /**
     * Represents an error returned by the SurrealDB server in an RPC response.
     */
    class SurrealDBException(val code: Int, message: String) : RuntimeException("SurrealDB Error $code: $message")

    /**
     * Represents a connection or client-side error during communication.
     */
    class ConnectionError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

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
    suspend fun <T> insert(table: String, data: T): List<*>? {
        // Data T must be encodable by SurrealCbor.cbor.encode.
        // If T is a data class, ensure customEnHook handles mapping it to a Map.
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
    suspend fun <T> update(entity: String, data: T): List<*>? {
        // Data T must be encodable by SurrealCbor.cbor.encode.
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
    suspend fun <T> upsert(entity: String, data: T): List<*>? {
        // Data T must be encodable by SurrealCbor.cbor.encode.
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
     * @param callback callback function to execute when query results change. Can be a regular or suspend function `(LiveQueryUpdate) -> Unit` or `suspend (LiveQueryUpdate) -> Unit`.
     * @param isAsync ignored - callbacks are always dispatched asynchronously in the client's CoroutineScope.
     * @return live query ID string.
     * @throws SurrealDBException if SurrealDB returns an error.
     * @throws ConnectionError if the connection fails.
     * @throws RuntimeException if the response from the server is not the expected string ID.
     */
    suspend fun live(entity: String, callback: Any): String { // Callback type is Any to allow both suspend and non-suspend lambdas
        val responseResult = sendRpc("live", listOf(entity))

        if (responseResult is String) {
            // Server returned the live query ID string
            attachLiveQuery(responseResult, callback) // Attach the provided callback
            return responseResult // Return the query ID
        } else {
            // Unexpected response format
            throw RuntimeException("Failed to start live query: Expected string ID but received $responseResult")
        }
    }

    /**
     * Attach a callback function to a live query ID that was previously started
     * (e.g., by executing a `LIVE SELECT ...` query via the [query] method).
     * @param queryId live query ID obtained from the server response.
     * @param callback callback function to execute when query results change. Can be a regular or suspend function `(LiveQueryUpdate) -> Unit` or `suspend (LiveQueryUpdate) -> Unit`.
     * @param isAsync ignored - callbacks are always dispatched asynchronously in the client's CoroutineScope.
     */
    suspend fun attachLiveQuery(queryId: String, callback: Any, isAsync: Boolean = false) {
        val callbackType = when (callback) {
            is Function1<*, *> -> RegularCallbackType()
            else -> SuspendCallbackType() // Nehme an, dass es eine Suspend-Funktion ist, wenn nicht Function1
        }

        liveQueriesMutex.withLock {
            liveQueries[queryId] = Triple(callback, isAsync, callbackType)
            logger.info { "Attached callback for live query ID: $queryId" }
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
     * Closes the WebSocket connection and cleans up all associated resources,
     * including cancelling pending requests and the client's CoroutineScope.
     * This should be called when the application is shutting down or the client is no longer needed.
     */
    fun close() {
        logger.info { "Closing SurrealDB client..." }
        // Cancel the CoroutineScope, which will cancel all coroutines launched within it
        clientScope.cancel()
        attemptReconnect.set(false)

        // Close the WebSocket connection gracefully
        webSocketClient?.let {
            if (!it.isClosing && !it.isClosed) {
                try {
                    it.close() // Initiate graceful closing
                } catch (e: Exception) {
                    logger.error { "Error closing WebSocket: ${e.message}" }
                }
            }
        }

        // Clean up internal state
        webSocketClient = null
        pendingRequests.clear()
        // Live queries map will be cleared by onClose callback or scope cancellation effect
        logger.info { "SurrealDB client closed." }
    }

    // It's good practice to provide a way to close explicitly.
    // While finalize can be a fallback, relying on it is not recommended.
    // protected fun finalize() {
    //     close()
    // }

    /**
     * Companion object for static factory methods.
     */
    companion object {
        /**
         * Create SurrealDB client from environment variables.
         * Required variables: SURREAL_ENDPOINT, SURREAL_USER, SURREAL_PASSWORD, SURREAL_NS, SURREAL_DB.
         *
         * @return SurrealDB client instance.
         * @throws IllegalArgumentException if environment variables are missing or endpoint is invalid.
         * @throws ConnectionError if connection or signin fails.
         */
        suspend fun fromEnv(): SurrealDBClient {
            val endpoint = System.getenv("SURREAL_ENDPOINT") ?: throw IllegalArgumentException("SURREAL_ENDPOINT environment variable not set.")
            val user = System.getenv("SURREAL_USER") ?: throw IllegalArgumentException("SURREAL_USER environment variable not set.")
            val password = System.getenv("SURREAL_PASSWORD") ?: throw IllegalArgumentException("SURREAL_PASSWORD environment variable not set.")
            val ns = System.getenv("SURREAL_NS") ?: throw IllegalArgumentException("SURREAL_NS environment variable not set.")
            val db = System.getenv("SURREAL_DB") ?: throw IllegalArgumentException("SURREAL_DB environment variable not set.")

            val client = SurrealDBClient(endpoint, user, password)
            // Wait for initial connection and signin before using
            client.ensureConnectedAndReady() // This will throw if connection/signin failed
            client.use(ns, db) // Set namespace and database
            return client
        }
    }
}

// Add this data class definition if it's not already present in ru.gd_alt.youwilldrive.data.models
// This structure is based on the expected payload for a live query update from SurrealDB

data class LiveQueryUpdate(
    val id: String, // The ID of the record that changed (e.g., "table:id")
    val action: String, // The type of change (e.g., "CREATE", "UPDATE", "DELETE")
    val result: Any? // The data associated with the change (e.g., the new/updated record, null for DELETE)
)


fun main() {
    // Example usage of the SurrealDBClient
    runBlocking {
        try {
            val client = SurrealDBClient(
                "ws://87.242.117.89:5457/rpc",
                "root",
                "iwilldrive"
            ) { client ->
                // This is the onConnect callback
                println("Connected to SurrealDB ${client.version()}")
                client.use("main", "main")
            }
            println("Connected to SurrealDB successfully!")
            client.close()
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}
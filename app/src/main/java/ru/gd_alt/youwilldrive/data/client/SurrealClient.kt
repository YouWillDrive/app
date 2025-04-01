package ru.gd_alt.youwilldrive.data.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.http.append
import io.ktor.http.headers
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray


@OptIn(ExperimentalSerializationApi::class)
object SurrealClient {
    const val SURREAL_HOST = "srl.gd-alt.ru"
    const val SURREAL_PORT = 5457
    const val SURREAL_PATH = "/rpc/"

    var counter = 1
    var queue: MutableMap<Int, Any?> = mutableMapOf()
    var callbacks = mutableMapOf<Int, (Any?) -> Unit>()

    private var _session: DefaultClientWebSocketSession? = null
    private val isListening = AtomicBoolean(false)
    private var listenerJob: Job? = null
    private val listenerScope = CoroutineScope(Dispatchers.IO)

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
            headers {
                append("Sec-WebSocket-Protocol", "cbor")
            }
        }
    }

    suspend fun getSession(): DefaultClientWebSocketSession? {
        if (_session == null) {
            _session = client.webSocketSession(
                method = HttpMethod.Get,
                host = SURREAL_HOST,
                port = SURREAL_PORT,
                path = SURREAL_PATH
            )
            startListener()
        }
        return _session!!
    }

    suspend fun closeSession() {
        _session?.close()
        _session = null
    }

    suspend fun exit() {
        _session?.close()
        client.close()
    }


    private fun startListener() {
        if (isListening.getAndSet(true)) {
            return
        }

        listenerJob = listenerScope.launch {
            try {
                val session = _session ?: return@launch
                while (isActive && session.isActive) {
                    val frame = session.incoming.receive()
                    when (frame) {
                        is Frame.Binary -> {
                            val responseBytes = frame.readBytes()
                            try {
                                val response = Cbor.decodeFromByteArray<Map<String, Any>>(responseBytes)
                                handleResponse(response)
                            } catch (e: Exception) {
                                println("Failure decoding CBOR: ${e.message}")
                            }
                        }
                        is Frame.Text -> {
                            val text = frame.toString()
                            println("Text messaage: $text")
                        }
                        else -> {
                            println("Unknown frame: ${frame::class.simpleName}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("WebSocket listener failed: ${e.message}")
            } finally {
                isListening.set(false)
            }
        }
    }

    private fun handleResponse(response: Map<String, Any>) {
        val id = response["id"] as? Int
        if (id != null) {
            queue[id] = response
        } else {
            println("No ID: $response")
        }
    }

    private suspend fun expectResponse(id: Int): Any? {
        while (queue[id] == null) {
            delay(50)
        }
        return queue.remove(id)
    }

    private suspend fun sendRequest(
        method: String,
        params: List<Any>
    ) {
        val session = getSession() ?: return
        val requestId = counter++
        val request = mapOf(
            "id" to requestId,
            "method" to method,
            "params" to params
        )

        session.send(Frame.Binary(true, Cbor.encodeToByteArray(request)))
        val response = expectResponse(requestId)
        callbacks[requestId]?.invoke(response)
    }
}
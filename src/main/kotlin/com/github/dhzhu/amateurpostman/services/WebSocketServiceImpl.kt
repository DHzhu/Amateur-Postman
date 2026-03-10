package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.WebSocketMessage
import com.github.dhzhu.amateurpostman.models.WebSocketMessageType
import com.github.dhzhu.amateurpostman.models.WebSocketState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Implementation of WebSocketService using OkHttp.
 *
 * Thread-safe WebSocket client with Kotlin Flow-based state management.
 */
class WebSocketServiceImpl : WebSocketService {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    // State management
    private val _state = MutableStateFlow(WebSocketState.DISCONNECTED)
    override val state: StateFlow<WebSocketState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = 64)
    override val messages: SharedFlow<WebSocketMessage> = _messages.asSharedFlow()

    private val _messageHistory = mutableListOf<WebSocketMessage>()
    override val messageHistory: List<WebSocketMessage> get() = _messageHistory.toList()

    private var _sentCount = 0
    override val sentCount: Int get() = _sentCount

    private var _receivedCount = 0
    override val receivedCount: Int get() = _receivedCount

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                _state.value = WebSocketState.CONNECTED
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                val message = WebSocketMessage(
                    content = text,
                    type = WebSocketMessageType.TEXT,
                    isOutgoing = false
                )
                _receivedCount++
                _messageHistory.add(message)
                _messages.emit(message)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
            scope.launch {
                val message = WebSocketMessage(
                    content = Base64.getEncoder().encodeToString(bytes.toByteArray()),
                    type = WebSocketMessageType.BINARY,
                    isOutgoing = false
                )
                _receivedCount++
                _messageHistory.add(message)
                _messages.emit(message)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            scope.launch {
                _state.value = WebSocketState.CLOSING
            }
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            scope.launch {
                _state.value = WebSocketState.CLOSED
                this@WebSocketServiceImpl.webSocket = null
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            scope.launch {
                _state.value = WebSocketState.DISCONNECTED
                this@WebSocketServiceImpl.webSocket = null
                // Emit error as a system message (could be handled differently)
                val errorMessage = WebSocketMessage(
                    content = "Connection error: ${t.message}",
                    type = WebSocketMessageType.TEXT,
                    isOutgoing = false
                )
                _messageHistory.add(errorMessage)
                _messages.emit(errorMessage)
            }
        }
    }

    override fun connect(url: String, headers: Map<String, String>) {
        if (_state.value == WebSocketState.CONNECTED || _state.value == WebSocketState.CONNECTING) {
            return
        }

        _state.value = WebSocketState.CONNECTING

        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        webSocket = client.newWebSocket(requestBuilder.build(), listener)
    }

    override fun send(message: String) {
        val ws = webSocket ?: return
        if (_state.value != WebSocketState.CONNECTED) return

        ws.send(message)
        val wsMessage = WebSocketMessage(
            content = message,
            type = WebSocketMessageType.TEXT,
            isOutgoing = true
        )
        _sentCount++
        _messageHistory.add(wsMessage)
        scope.launch {
            _messages.emit(wsMessage)
        }
    }

    override fun sendBinary(data: ByteArray) {
        val ws = webSocket ?: return
        if (_state.value != WebSocketState.CONNECTED) return

        val byteString = okio.ByteString.of(*data)
        ws.send(byteString)
        val wsMessage = WebSocketMessage(
            content = Base64.getEncoder().encodeToString(data),
            type = WebSocketMessageType.BINARY,
            isOutgoing = true
        )
        _sentCount++
        _messageHistory.add(wsMessage)
        scope.launch {
            _messages.emit(wsMessage)
        }
    }

    override fun disconnect() {
        val ws = webSocket ?: return
        if (_state.value == WebSocketState.DISCONNECTED ||
            _state.value == WebSocketState.CLOSED ||
            _state.value == WebSocketState.CLOSING) {
            return
        }

        _state.value = WebSocketState.CLOSING
        ws.close(1000, "Client closed connection")
    }

    override fun clearHistory() {
        _messageHistory.clear()
        _sentCount = 0
        _receivedCount = 0
    }

    /**
     * Cleanup resources. Should be called when the service is no longer needed.
     */
    fun dispose() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }
}
package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.WebSocketMessage
import com.github.dhzhu.amateurpostman.models.WebSocketState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service interface for WebSocket connections.
 *
 * Provides connection management and message handling for WebSocket clients.
 * Uses Kotlin Flows for reactive state observation.
 */
interface WebSocketService {

    /**
     * Current connection state as a StateFlow.
     * UI components can observe this to update their state accordingly.
     */
    val state: StateFlow<WebSocketState>

    /**
     * Stream of received/sent messages.
     * New messages are emitted to this flow as they arrive or are sent.
     */
    val messages: SharedFlow<WebSocketMessage>

    /**
     * List of all messages in the current session.
     * Cleared when clearHistory() is called or on disconnect.
     */
    val messageHistory: List<WebSocketMessage>

    /**
     * Count of sent messages in the current session.
     */
    val sentCount: Int

    /**
     * Count of received messages in the current session.
     */
    val receivedCount: Int

    /**
     * Connect to a WebSocket server.
     *
     * @param url The WebSocket URL (ws:// or wss://)
     * @param headers Optional HTTP headers to send during the handshake
     */
    fun connect(url: String, headers: Map<String, String> = emptyMap())

    /**
     * Send a text message to the connected server.
     *
     * @param message The text message to send
     */
    fun send(message: String)

    /**
     * Send a binary message to the connected server.
     *
     * @param data The binary data to send
     */
    fun sendBinary(data: ByteArray)

    /**
     * Disconnect from the WebSocket server.
     * Initiates a graceful close handshake.
     */
    fun disconnect()

    /**
     * Clear the message history.
     * Does not affect the connection state.
     */
    fun clearHistory()
}
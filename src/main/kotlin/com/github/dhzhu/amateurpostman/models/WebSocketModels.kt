package com.github.dhzhu.amateurpostman.models

import com.github.dhzhu.amateurpostman.ui.StreamMessage

/**
 * WebSocket connection state enum
 */
enum class WebSocketState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSING,
    CLOSED
}

/**
 * WebSocket message types
 */
enum class WebSocketMessageType {
    TEXT,
    BINARY
}

/**
 * Message record for WebSocket history
 *
 * @param content The message content (String for text, Base64-encoded string for binary)
 * @param type Whether this is a text or binary message
 * @param isOutgoing true = sent by client, false = received from server
 * @param timestamp Unix timestamp in milliseconds
 */
data class WebSocketMessage(
    override val content: String,
    val type: WebSocketMessageType,
    override val isOutgoing: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
) : StreamMessage

/**
 * WebSocket connection configuration
 *
 * @param url The WebSocket URL (ws:// or wss://)
 * @param headers Optional HTTP headers to send during the handshake
 */
data class WebSocketConnection(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)
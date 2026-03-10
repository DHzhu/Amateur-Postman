package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.WebSocketMessageType
import com.github.dhzhu.amateurpostman.models.WebSocketState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for WebSocketServiceImpl using MockWebServer.
 *
 * Note: These tests verify basic WebSocket functionality with a mock server.
 */
class WebSocketServiceIntegrationTest {

    private lateinit var service: WebSocketServiceImpl
    private lateinit var mockServer: MockWebServer

    @BeforeEach
    fun setup() {
        service = WebSocketServiceImpl()
        mockServer = MockWebServer()
    }

    @AfterEach
    fun teardown() {
        try {
            service.dispose()
        } catch (_: Exception) {}
        try {
            mockServer.shutdown()
        } catch (_: Exception) {}
    }

    @Test
    fun `connect to mock WebSocket server succeeds`() {
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(createSilentWebSocketListener()))

        val serverUrl = "ws://${mockServer.hostName}:${mockServer.port}/"

        runBlocking {
            withTimeout(5000) {
                service.connect(serverUrl)
                service.state.first { it == WebSocketState.CONNECTED }
            }
        }

        assertEquals(WebSocketState.CONNECTED, service.state.value)
    }

    @Test
    fun `send binary message when connected`() {
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(createSilentWebSocketListener()))

        val serverUrl = "ws://${mockServer.hostName}:${mockServer.port}/"

        runBlocking {
            withTimeout(5000) {
                service.connect(serverUrl)
                service.state.first { it == WebSocketState.CONNECTED }
            }

            val binaryData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            service.sendBinary(binaryData)
        }

        assertEquals(1, service.sentCount)
        val lastMessage = service.messageHistory.last()
        assertTrue(lastMessage.isOutgoing)
        assertEquals(WebSocketMessageType.BINARY, lastMessage.type)
    }

    @Test
    fun `receive multiple messages from server`() {
        val messageCount = 5

        mockServer.enqueue(MockResponse().withWebSocketUpgrade(createMultiMessageWebSocketListener(messageCount)))

        val serverUrl = "ws://${mockServer.hostName}:${mockServer.port}/"

        runBlocking {
            withTimeout(5000) {
                service.connect(serverUrl)
                service.state.first { it == WebSocketState.CONNECTED }
            }

            // Wait for all messages
            withTimeout(5000) {
                while (service.receivedCount < messageCount) {
                    delay(50)
                }
            }
        }

        assertEquals(messageCount, service.receivedCount)
    }

    @Test
    fun `handle connection failure gracefully`() {
        // Connect to an invalid port
        val invalidUrl = "ws://localhost:1/invalid"

        runBlocking {
            service.connect(invalidUrl)

            // Should eventually return to DISCONNECTED state
            withTimeout(10000) {
                service.state.first { it == WebSocketState.DISCONNECTED }
            }
        }

        assertEquals(WebSocketState.DISCONNECTED, service.state.value)
    }

    @Test
    fun `send text message when connected`() {
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(createSilentWebSocketListener()))

        val serverUrl = "ws://${mockServer.hostName}:${mockServer.port}/"

        runBlocking {
            withTimeout(5000) {
                service.connect(serverUrl)
                service.state.first { it == WebSocketState.CONNECTED }
            }

            service.send("Hello WebSocket")
        }

        assertEquals(1, service.sentCount)
        val lastMessage = service.messageHistory.last()
        assertTrue(lastMessage.isOutgoing)
        assertEquals("Hello WebSocket", lastMessage.content)
    }

    // ─── WebSocket Listener Factories ─────────────────────────────────────────

    private fun createSilentWebSocketListener(): okhttp3.WebSocketListener {
        return object : okhttp3.WebSocketListener() {}
    }

    private fun createMultiMessageWebSocketListener(count: Int): okhttp3.WebSocketListener {
        return object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                repeat(count) { i ->
                    webSocket.send("Message $i")
                }
            }
        }
    }
}
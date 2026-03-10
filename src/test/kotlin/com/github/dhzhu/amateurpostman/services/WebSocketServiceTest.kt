package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.WebSocketMessageType
import com.github.dhzhu.amateurpostman.models.WebSocketState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebSocketServiceTest {

    private lateinit var service: WebSocketServiceImpl

    @BeforeEach
    fun setup() {
        service = WebSocketServiceImpl()
    }

    @AfterEach
    fun teardown() {
        try {
            service.dispose()
        } catch (_: Exception) {}
    }

    @Test
    fun `initial state is DISCONNECTED`() {
        assertEquals(WebSocketState.DISCONNECTED, service.state.value)
    }

    @Test
    fun `initial counts are zero`() {
        assertEquals(0, service.sentCount)
        assertEquals(0, service.receivedCount)
        assertTrue(service.messageHistory.isEmpty())
    }

    @Test
    fun `cannot send when disconnected`() {
        service.send("Test message")
        assertEquals(0, service.sentCount)
        assertTrue(service.messageHistory.isEmpty())
    }

    @Test
    fun `cannot send binary when disconnected`() {
        service.sendBinary(byteArrayOf(0x01, 0x02))
        assertEquals(0, service.sentCount)
        assertTrue(service.messageHistory.isEmpty())
    }

    @Test
    fun `clearHistory resets counts and history`() {
        service.clearHistory()
        assertEquals(0, service.sentCount)
        assertEquals(0, service.receivedCount)
        assertTrue(service.messageHistory.isEmpty())
    }

    @Test
    fun `state flow is observable`() {
        var observedState: WebSocketState? = null
        val job = GlobalScope.launch {
            service.state.collect { observedState = it }
        }

        runBlocking {
            delay(100)
        }

        assertEquals(WebSocketState.DISCONNECTED, observedState)
        job.cancel()
    }

    @Test
    fun `connect does not change state when URL is invalid`() {
        // Connect to an invalid URL - should fail gracefully
        service.connect("ws://nonexistent.local")

        // State should eventually go back to DISCONNECTED after failure
        runBlocking {
            withTimeout(5000) {
                service.state.first { it == WebSocketState.DISCONNECTED }
            }
        }

        assertEquals(WebSocketState.DISCONNECTED, service.state.value)
    }

    @Test
    fun `multiple clearHistory calls are safe`() {
        service.clearHistory()
        service.clearHistory()
        service.clearHistory()

        assertEquals(0, service.sentCount)
        assertEquals(0, service.receivedCount)
        assertTrue(service.messageHistory.isEmpty())
    }

    @Test
    fun `dispose is safe to call multiple times`() {
        service.dispose()
        service.dispose()
        // Should not throw
    }
}
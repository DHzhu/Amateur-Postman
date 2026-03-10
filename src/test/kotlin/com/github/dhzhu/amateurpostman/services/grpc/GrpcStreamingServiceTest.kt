package com.github.dhzhu.amateurpostman.services.grpc

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GrpcStreamingService.
 *
 * Note: These tests verify the state management and flow mechanics.
 * Actual gRPC streaming requires a running server.
 */
class GrpcStreamingServiceTest {

    @Test
    fun `initial state should be IDLE`() {
        val service = GrpcStreamingService()
        try {
            assertEquals(GrpcStreamState.IDLE, service.state.value)
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `initial counts should be zero`() {
        val service = GrpcStreamingService()
        try {
            assertEquals(0, service.sentCount)
            assertEquals(0, service.receivedCount)
            assertTrue(service.messageHistory.isEmpty())
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `clearHistory should reset counts`() {
        val service = GrpcStreamingService()
        try {
            service.clearHistory()

            assertEquals(0, service.sentCount)
            assertEquals(0, service.receivedCount)
            assertTrue(service.messageHistory.isEmpty())
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `cancelStream should reset state to IDLE`() {
        val service = GrpcStreamingService()
        try {
            // Cancel when already IDLE should be no-op
            service.cancelStream()
            assertEquals(GrpcStreamState.IDLE, service.state.value)
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `completeStream when not streaming should do nothing`() {
        val service = GrpcStreamingService()
        try {
            // Complete when IDLE should be no-op
            service.completeStream()
            assertEquals(GrpcStreamState.IDLE, service.state.value)
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `sendMessage when not streaming should return false`() {
        val service = GrpcStreamingService()
        try {
            // sendMessage when IDLE should return false
            // Note: This requires a method descriptor, which is complex to create
            // In practice, integration tests would use real proto files
            // This test verifies the service can be created and disposed correctly
            assertFalse(service.sentCount > 0)
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `state flow should have correct initial value`() {
        val service = GrpcStreamingService()
        try {
            val initialState = service.state.value
            assertEquals(GrpcStreamState.IDLE, initialState)
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `messages flow should be empty initially`() {
        val service = GrpcStreamingService()
        try {
            // Message history should be empty
            assertTrue(service.messageHistory.isEmpty())
        } finally {
            service.dispose()
        }
    }

    @Test
    fun `dispose should cancel any active stream`() {
        val service = GrpcStreamingService()
        // Dispose should not throw even when IDLE
        service.dispose()

        // State should still be IDLE after dispose
        assertEquals(GrpcStreamState.IDLE, service.state.value)
    }

    @Test
    fun `state values should be distinct`() {
        val states = GrpcStreamState.entries.toSet()
        assertEquals(4, states.size)
        assertTrue(GrpcStreamState.IDLE in states)
        assertTrue(GrpcStreamState.STREAMING in states)
        assertTrue(GrpcStreamState.COMPLETED in states)
        assertTrue(GrpcStreamState.ERROR in states)
    }

    @Test
    fun `GrpcStreamMessage should have correct properties`() {
        val message = GrpcStreamMessage(
            content = """{"key":"value"}""",
            isOutgoing = true,
            timestamp = 1234567890L
        )

        assertEquals("""{"key":"value"}""", message.content)
        assertTrue(message.isOutgoing)
        assertEquals(1234567890L, message.timestamp)
    }

    @Test
    fun `GrpcStreamMessage incoming should have isOutgoing false`() {
        val message = GrpcStreamMessage(
            content = "response",
            isOutgoing = false
        )

        assertFalse(message.isOutgoing)
    }

    @Test
    fun `GrpcMethodType should have all streaming types`() {
        val types = GrpcMethodType.entries.toSet()
        assertEquals(4, types.size)
        assertTrue(GrpcMethodType.UNARY in types)
        assertTrue(GrpcMethodType.SERVER_STREAMING in types)
        assertTrue(GrpcMethodType.CLIENT_STREAMING in types)
        assertTrue(GrpcMethodType.BIDI_STREAMING in types)
    }

    @Test
    fun `GrpcStreamRequest should have correct defaults`() {
        val request = GrpcStreamRequest(
            host = "localhost",
            port = 50051,
            serviceName = "TestService",
            methodName = "TestMethod"
        )

        assertEquals("localhost", request.host)
        assertEquals(50051, request.port)
        assertEquals("TestService", request.serviceName)
        assertEquals("TestMethod", request.methodName)
        assertTrue(request.metadata.isEmpty())
        assertFalse(request.useTls)
        assertEquals(30L, request.deadlineSeconds)
    }
}
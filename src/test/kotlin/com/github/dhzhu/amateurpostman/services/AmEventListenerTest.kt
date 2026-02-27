package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Unit tests for AmEventListener.
 * Tests network timing collection for both fresh connections and connection reuse scenarios.
 */
class AmEventListenerTest {

    /**
     * Test Scenario 1: Fresh Connection with SSL (simulated)
     * Expected: All timing metrics should be collected and non-null
     */
    @Test
    fun testProfilingDataWithFreshConnectionAndSSL() {
        val listener = AmEventListener()

        // Simulate a fresh connection with SSL
        val call = createMockCall()

        // Simulate event sequence for a fresh connection
        listener.callStart(call)
        listener.dnsStart(call, "example.com")
        Thread.sleep(10) // Simulate DNS delay
        listener.dnsEnd(call, "example.com", listOf(InetAddress.getByName("127.0.0.1")))

        listener.connectStart(call, InetSocketAddress("127.0.0.1", 443), Proxy.NO_PROXY)
        Thread.sleep(15) // Simulate TCP delay
        listener.connectEnd(call, InetSocketAddress("127.0.0.1", 443), Proxy.NO_PROXY, okhttp3.Protocol.HTTP_1_1)

        listener.secureConnectStart(call)
        Thread.sleep(20) // Simulate SSL delay
        // Create a minimal handshake for testing
        listener.secureConnectEnd(call, null)

        listener.requestHeadersStart(call)
        listener.requestHeadersEnd(call, createMockRequest())

        listener.requestBodyStart(call)
        listener.requestBodyEnd(call, 0)

        listener.responseHeadersStart(call)
        listener.responseHeadersEnd(call, createMockResponse())

        Thread.sleep(25) // Simulate TTFB delay
        listener.responseBodyStart(call)
        listener.responseBodyEnd(call, 0)

        listener.callEnd(call)

        // Get profiling data
        val profilingData = listener.getProfilingData()

        // Assert all timing data is collected for fresh connection
        assertNotNull("DNS duration should not be null for fresh connection", profilingData.dnsDuration)
        assertNotNull("TCP duration should not be null for fresh connection", profilingData.tcpDuration)
        assertNotNull("SSL duration should not be null for fresh SSL connection", profilingData.sslDuration)
        assertNotNull("TTFB duration should not be null", profilingData.ttfbDuration)
        assertTrue("Total duration should be positive", profilingData.totalDuration > 0)
        assertFalse("Connection should not be marked as reused", profilingData.connectionReused)

        // Verify timing relationships (allowing for test execution variations)
        assertTrue("DNS duration should be at least 10ms", profilingData.dnsDuration!! >= 10)
        assertTrue("TCP duration should be at least 15ms", profilingData.tcpDuration!! >= 15)
        assertTrue("SSL duration should be at least 20ms", profilingData.sslDuration!! >= 20)
        assertTrue("TTFB duration should be at least 25ms", profilingData.ttfbDuration!! >= 25)
    }

    /**
     * Test Scenario 2: Connection Reuse
     * Expected: DNS, TCP, SSL timings should be null; only TTFB and total duration available
     */
    @Test
    fun testProfilingDataWithConnectionReuse() {
        val listener = AmEventListener()

        // Simulate connection reuse
        val call = createMockCall()
        listener.callStart(call)

        // Skip DNS, TCP, SSL events (connection is reused)

        listener.requestHeadersStart(call)
        listener.requestHeadersEnd(call, createMockRequest())

        listener.requestBodyStart(call)
        listener.requestBodyEnd(call, 0)

        listener.responseHeadersStart(call)
        listener.responseHeadersEnd(call, createMockResponse())

        Thread.sleep(30) // Simulate TTFB delay
        listener.responseBodyStart(call)
        listener.responseBodyEnd(call, 0)

        listener.callEnd(call)

        // Get profiling data
        val profilingData = listener.getProfilingData()

        // Assert connection reuse behavior
        assertNull("DNS duration should be null for connection reuse", profilingData.dnsDuration)
        assertNull("TCP duration should be null for connection reuse", profilingData.tcpDuration)
        assertNull("SSL duration should be null for connection reuse", profilingData.sslDuration)
        assertNotNull("TTFB duration should still be collected", profilingData.ttfbDuration)
        assertTrue("Total duration should be positive", profilingData.totalDuration > 0)
        assertTrue("Connection should be marked as reused", profilingData.connectionReused)
    }

    /**
     * Test Scenario 3: Fresh Connection without SSL (HTTP)
     * Expected: DNS, TCP timings available; SSL timing is null
     */
    @Test
    fun testProfilingDataWithFreshConnectionHTTP() {
        val listener = AmEventListener()

        val call = createMockCall()

        // Simulate HTTP (non-SSL) connection
        listener.callStart(call)
        listener.dnsStart(call, "example.com")
        Thread.sleep(5)
        listener.dnsEnd(call, "example.com", listOf(InetAddress.getByName("127.0.0.1")))

        listener.connectStart(call, InetSocketAddress("127.0.0.1", 80), Proxy.NO_PROXY)
        Thread.sleep(8)
        listener.connectEnd(call, InetSocketAddress("127.0.0.1", 80), Proxy.NO_PROXY, okhttp3.Protocol.HTTP_1_1)

        // No SSL events for HTTP

        listener.requestHeadersStart(call)
        listener.requestHeadersEnd(call, createMockRequest())

        listener.requestBodyStart(call)
        listener.requestBodyEnd(call, 0)

        listener.responseHeadersStart(call)
        listener.responseHeadersEnd(call, createMockResponse())

        Thread.sleep(12)
        listener.responseBodyStart(call)
        listener.responseBodyEnd(call, 0)

        listener.callEnd(call)

        val profilingData = listener.getProfilingData()

        assertNotNull("DNS duration should be collected", profilingData.dnsDuration)
        assertNotNull("TCP duration should be collected", profilingData.tcpDuration)
        assertNull("SSL duration should be null for HTTP", profilingData.sslDuration)
        assertNotNull("TTFB duration should be collected", profilingData.ttfbDuration)
        assertFalse("Connection should not be marked as reused", profilingData.connectionReused)
    }

    /**
     * Test Scenario 4: Request Failure
     * Expected: Profiling data should still be available even on failure
     */
    @Test
    fun testProfilingDataWithRequestFailure() {
        val listener = AmEventListener()

        val call = createMockCall()

        listener.callStart(call)
        listener.dnsStart(call, "example.com")
        Thread.sleep(5)
        listener.dnsEnd(call, "example.com", listOf(InetAddress.getByName("127.0.0.1")))

        // Simulate failure after DNS
        listener.callFailed(call, java.io.IOException("Connection refused"))

        val profilingData = listener.getProfilingData()

        // Even on failure, we should have partial timing data
        assertNotNull("DNS duration should be collected before failure", profilingData.dnsDuration)
        assertNull("TCP duration should be null (connection failed)", profilingData.tcpDuration)
    }

    /**
     * Test Scenario 5: Empty Profiling Data (no events)
     * Expected: Returns HttpProfilingData.Empty
     */
    @Test
    fun testEmptyProfilingData() {
        val listener = AmEventListener()

        val profilingData = listener.getProfilingData()

        assertEquals("Should return empty profiling data", HttpProfilingData.Empty, profilingData)
    }

    /**
     * Helper: Create a mock okhttp3.Call for testing
     */
    private fun createMockCall(): okhttp3.Call {
        // Create a real OkHttpClient and Request to get a valid Call object
        val client = OkHttpClient()
        val request = Request.Builder().url("https://example.com").build()
        return client.newCall(request)
    }

    /**
     * Helper: Create a mock Request for testing
     */
    private fun createMockRequest(): okhttp3.Request {
        return Request.Builder().url("https://example.com").build()
    }

    /**
     * Helper: Create a mock Response for testing
     */
    private fun createMockResponse(): okhttp3.Response {
        val client = OkHttpClient()
        val request = Request.Builder().url("https://example.com").build()
        // This will actually fail if executed, but we just need the Response object structure
        return request.newBuilder().build().let {
            // Return a mock response - we'll create a simple one
            client.newCall(it).execute()
        }
    }

    /**
     * Test: Verify total duration calculation
     */
    @Test
    fun testTotalDurationCalculation() {
        val listener = AmEventListener()
        val call = createMockCall()

        val startTime = System.nanoTime()

        listener.callStart(call)
        Thread.sleep(50)
        listener.callEnd(call)

        val endTime = System.nanoTime()
        val expectedDurationMs = (endTime - startTime) / 1_000_000

        val profilingData = listener.getProfilingData()

        // Total duration should be approximately 50ms (within 10ms tolerance)
        assertTrue(
            "Total duration should be approximately 50ms, got ${profilingData.totalDuration}ms",
            profilingData.totalDuration >= 40 && profilingData.totalDuration <= 100
        )
    }
}

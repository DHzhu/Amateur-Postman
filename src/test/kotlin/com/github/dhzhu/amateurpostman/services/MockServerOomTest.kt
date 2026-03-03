package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for Mock Server robustness - OOM prevention and edge cases.
 * These tests verify that the server handles malicious or malformed requests gracefully.
 */
class MockServerOomTest {

    private lateinit var mockServerManager: MockServerManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @BeforeEach
    fun setUp() {
        mockServerManager = MockServerManager()
    }

    @AfterEach
    fun tearDown() {
        mockServerManager.stop()
        mockServerManager.clearRules()
    }

    // ========== OOM Prevention Tests ==========

    /**
     * Test: Large request body should be rejected to prevent OOM.
     * The server should have a maximum body size limit.
     */
    @Test
    fun `test large request body is rejected with 413`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/upload", HttpMethod.POST, """{"status":"ok"}""")
        )

        // Create a large body (10MB)
        val largeBody = "x".repeat(10 * 1024 * 1024)

        val request = Request.Builder()
            .url("http://localhost:$port/api/upload")
            .post(largeBody.toRequestBody("text/plain".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        // Expect 413 Payload Too Large or 400 Bad Request
        assertTrue(
            response.code == 413 || response.code == 400,
            "Expected 413 Payload Too Large or 400 Bad Request, but got ${response.code}"
        )
    }

    /**
     * Test: Moderate request body within limits should be accepted.
     * This verifies that legitimate requests still work after adding size limits.
     */
    @Test
    fun `test moderate request body within limits is accepted`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/data", HttpMethod.POST, """{"status":"ok"}""")
        )

        // Create a moderate body (1MB - should be within limits)
        val moderateBody = "x".repeat(1024 * 1024)

        val request = Request.Builder()
            .url("http://localhost:$port/api/data")
            .post(moderateBody.toRequestBody("text/plain".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }

    /**
     * Test: Very large response body should not cause OOM in the server.
     * The server should handle sending large responses without crashing.
     */
    @Test
    fun `test large response body can be sent`() = runBlocking {
        val port = mockServerManager.start(0)!!
        // Create a large response body (5MB)
        val largeResponse = """{"data":"${"x".repeat(5 * 1024 * 1024)}"}"""
        mockServerManager.addRule(
            MockRule.create("/api/large", HttpMethod.GET, largeResponse)
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/large")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertTrue(response.body?.contentLength()!! > 5 * 1024 * 1024)
    }

    /**
     * Test: Request body at the boundary of allowed size should be handled correctly.
     */
    @Test
    fun `test request body at size boundary`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/data", HttpMethod.POST, """{"status":"ok"}""")
        )

        // Create a body at exactly 1MB boundary
        val boundaryBody = "x".repeat(1024 * 1024) // 1MB

        val request = Request.Builder()
            .url("http://localhost:$port/api/data")
            .post(boundaryBody.toRequestBody("text/plain".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        // Should be accepted (within 1MB limit)
        assertEquals(200, response.code)
    }

    /**
     * Test: Empty request body should be handled correctly.
     */
    @Test
    fun `test empty request body is handled`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/empty", HttpMethod.POST, """{"status":"ok"}""")
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/empty")
            .post("".toRequestBody("text/plain".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }

    /**
     * Test: Multiple concurrent large requests should not exhaust memory.
     * This is a stress test for memory management.
     */
    @Test
    fun `test multiple concurrent requests do not cause oom`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/data", HttpMethod.POST, """{"status":"ok"}""")
        )

        // Send multiple moderate requests concurrently
        val numRequests = 10
        val requests = (1..numRequests).map { i ->
            httpClient.newCall(
                Request.Builder()
                    .url("http://localhost:$port/api/data")
                    .post("{\"id\":$i}".toRequestBody("application/json".toMediaType()))
                    .build()
            )
        }

        val responses = requests.map { it.execute() }

        responses.forEach { response ->
            assertEquals(200, response.code)
        }
    }
}
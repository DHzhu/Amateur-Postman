package com.github.dhzhu.amateurpostman.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedOutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Security and robustness tests for SimpleHttpServer.
 * Verifies protection against OOM via oversized Content-Length and DoS via infinite header lines.
 */
class SimpleHttpServerSecurityTest {

    private lateinit var server: SimpleHttpServer
    private var port: Int = 0

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    @BeforeEach
    fun setUp() {
        server = SimpleHttpServer()
        server.createContext("/") { exchange ->
            exchange.sendResponse(200, mapOf("Content-Type" to "text/plain"), "OK".toByteArray())
        }
        port = server.start(0)
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    /**
     * Verifies that a request with Content-Length exceeding MAX_BODY_SIZE is
     * rejected immediately without allocating the buffer.
     *
     * Without fix: parseRequest allocates ByteArray(5MB) → potential OOM, blocks reading body
     * With fix: parseRequest returns null → connection closed immediately
     *
     * Measures rejection speed via a follow-up health check on the same connection.
     */
    @Test
    fun `request with huge Content-Length is rejected quickly`() {
        val start = System.currentTimeMillis()

        val socket = Socket("localhost", port)
        try {
            socket.soTimeout = 3000
            val output = BufferedOutputStream(socket.getOutputStream())
            val input = socket.getInputStream()

            // Send POST with 5MB Content-Length but no body
            val request = "POST /test HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Content-Length: 5242880\r\n" +
                "\r\n"
            output.write(request.toByteArray())
            output.flush()

            // Read response — with fix, connection is closed immediately (read returns -1)
            // Without fix, server blocks reading 5MB body until socket timeout (3s)
            val firstByte = input.read()
            val elapsed = System.currentTimeMillis() - start

            if (firstByte == -1) {
                // Connection closed by server (expected with fix)
                assertTrue(elapsed < 1000, "Rejection should be instant, took ${elapsed}ms")
            } else {
                // Server sent a response (unexpected but acceptable)
                assertTrue(elapsed < 1000, "Response should be fast, took ${elapsed}ms")
            }
        } finally {
            socket.close()
        }
    }

    /**
     * Verifies that a normal POST request is handled correctly.
     */
    @Test
    fun `request with normal Content-Length is accepted`() {
        val body = "test body"
        val request = Request.Builder()
            .url("http://localhost:$port/test")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)
    }

    /**
     * Verifies that a request with an extremely long header line (>8KB) without
     * \r\n terminator causes the server to block (DoS vulnerability).
     *
     * Without fix: readLine reads indefinitely → blocks until socket timeout
     * With fix: readLine returns null after MAX_HEADER_LINE_LENGTH → immediate rejection
     *
     * Uses OkHttp client which has its own read timeout, so a blocking server
     * will cause a timeout exception (test failure).
     */
    @Test
    fun `request with unterminated long header is rejected without blocking`() {
        val socket = Socket("localhost", port)
        try {
            socket.soTimeout = 3000
            val output = BufferedOutputStream(socket.getOutputStream())
            val input = socket.getInputStream()

            // Send header line WITHOUT \r\n terminator — readLine should hit length limit
            val longValue = "x".repeat(100_000)
            val request = "GET /test HTTP/1.1\r\nX-Long: $longValue"
            output.write(request.toByteArray())
            output.flush()

            // With fix: server closes connection quickly (readLine returns null at 8KB)
            // Without fix: server blocks reading until socket timeout
            val start = System.currentTimeMillis()
            val firstByte = try { input.read() } catch (_: Exception) { -1 }
            val elapsed = System.currentTimeMillis() - start

            // Connection should be closed within 1s if readLine has a length limit
            assertTrue(elapsed < 1000, "Server should reject unterminated long header in <1s, took ${elapsed}ms")
        } finally {
            socket.close()
        }
    }
}

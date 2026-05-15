package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * Custom OkHttp EventListener for collecting HTTP network profiling data.
 *
 * This listener captures timing information for various stages of an HTTP request:
 * - DNS resolution
 * - TCP connection establishment
 * - SSL/TLS handshake (for HTTPS)
 * - Time to First Byte (TTFB)
 *
 * It handles both fresh connections and connection reuse scenarios.
 * For connection reuse, DNS, TCP, and SSL timings will be null.
 */
class AmEventListener : EventListener() {

    // Timing tracking variables
    private var callStartTimeNanos: Long = 0
    private var dnsStartTimeNanos: Long = 0
    private var connectStartTimeNanos: Long = 0
    private var secureConnectStartTimeNanos: Long = 0
    private var responseHeadersEndTimeNanos: Long = 0
    private var responseBodyStartTimeNanos: Long = 0

    // Collected durations (in milliseconds)
    private var dnsDurationMs: Long? = null
    private var tcpDurationMs: Long? = null
    private var sslDurationMs: Long? = null
    private var ttfbDurationMs: Long? = null
    private var totalDurationMs: Long = 0

    // Connection reuse tracking
    private var connectionReused: Boolean = true // Default to true (assume reuse until proven otherwise)

    /**
     * Returns the collected profiling data.
     * Returns HttpProfilingData.Empty if no timing data was collected.
     */
    fun getProfilingData(): HttpProfilingData {
        // If no events were triggered, return empty data
        if (callStartTimeNanos == 0L) {
            return HttpProfilingData.Empty
        }

        return HttpProfilingData(
            dnsDuration = dnsDurationMs,
            tcpDuration = tcpDurationMs,
            sslDuration = sslDurationMs,
            ttfbDuration = ttfbDurationMs,
            totalDuration = totalDurationMs,
            connectionReused = connectionReused
        )
    }

    override fun callStart(call: Call) {
        callStartTimeNanos = System.nanoTime()
    }

    override fun callEnd(call: Call) {
        totalDurationMs = nanosToMillis(System.nanoTime() - callStartTimeNanos)
    }

    override fun callFailed(call: Call, ioe: IOException) {
        // Calculate total duration even on failure
        if (callStartTimeNanos > 0) {
            totalDurationMs = nanosToMillis(System.nanoTime() - callStartTimeNanos)
        }
    }

    override fun dnsStart(call: Call, domainName: String) {
        dnsStartTimeNanos = System.nanoTime()
        // DNS start means we're not reusing a connection
        connectionReused = false
    }

    override fun dnsEnd(
        call: Call,
        domainName: String,
        inetAddressList: List<InetAddress>
    ) {
        if (dnsStartTimeNanos > 0) {
            dnsDurationMs = nanosToMillis(System.nanoTime() - dnsStartTimeNanos)
        }
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy
    ) {
        connectStartTimeNanos = System.nanoTime()
        // Connect start means we're not reusing a connection
        connectionReused = false
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: okhttp3.Protocol?
    ) {
        if (connectStartTimeNanos > 0) {
            tcpDurationMs = nanosToMillis(System.nanoTime() - connectStartTimeNanos)
        }
    }

    override fun secureConnectStart(call: Call) {
        secureConnectStartTimeNanos = System.nanoTime()
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        if (secureConnectStartTimeNanos > 0) {
            sslDurationMs = nanosToMillis(System.nanoTime() - secureConnectStartTimeNanos)
        }
    }

    override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
        // Request headers sent
    }

    override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
        responseHeadersEndTimeNanos = System.nanoTime()
    }

    override fun responseBodyStart(call: Call) {
        responseBodyStartTimeNanos = System.nanoTime()

        // Calculate TTFB: from call start to first byte of response body
        if (callStartTimeNanos > 0) {
            ttfbDurationMs = nanosToMillis(responseBodyStartTimeNanos - callStartTimeNanos)
        }
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        // Response body fully received
    }

    /**
     * Convert nanoseconds to milliseconds.
     * Ensures that any non-zero duration is at least 1ms for display.
     */
    private fun nanosToMillis(nanos: Long): Long {
        return if (nanos > 0) Math.max(1L, nanos / 1_000_000) else 0L
    }
}

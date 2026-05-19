package com.github.dhzhu.amateurpostman.utils

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Factory for creating pre-configured OkHttpClient instances.
 * Centralizes timeout and redirect configuration to avoid duplication across services.
 */
object OkHttpClientFactory {

    private const val DEFAULT_TIMEOUT_SECONDS = 30L

    /**
     * Creates a standard HTTP client builder with default timeouts (30s connect/read/write)
     * and redirect following enabled. Each service should build its own instance to manage
     * its own lifecycle (dispatcher, connection pool).
     */
    fun defaultBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)

    /**
     * Creates a WebSocket client builder with default timeouts and ping interval.
     */
    fun webSocketBuilder(pingIntervalSeconds: Long = 25): OkHttpClient.Builder = defaultBuilder()
        .pingInterval(pingIntervalSeconds, TimeUnit.SECONDS)
}

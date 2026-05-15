package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.BodyMatcher
import com.github.dhzhu.amateurpostman.models.BodyMatchMode
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import com.sun.net.httpserver.HttpServer
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Serializable state for BodyMatcher.
 */
data class BodyMatcherState(
    var mode: String = "NONE",
    var pattern: String = ""
) {
    fun toBodyMatcher(): BodyMatcher = BodyMatcher(
        mode = try { BodyMatchMode.valueOf(mode) } catch (e: Exception) { BodyMatchMode.NONE },
        pattern = pattern
    )

    companion object {
        fun fromBodyMatcher(matcher: BodyMatcher): BodyMatcherState = BodyMatcherState(
            mode = matcher.mode.name,
            pattern = matcher.pattern
        )
    }
}

/**
 * Persistent state for Mock rules.
 */
data class MockServerState(
    var rules: MutableList<MockRuleState> = mutableListOf()
)

/**
 * Serializable state for a single MockRule.
 */
data class MockRuleState(
    var id: String = "",
    var path: String = "",
    var method: String = "GET",
    var statusCode: Int = 200,
    var headers: MutableMap<String, String> = mutableMapOf(),
    var body: String = "",
    var delayMs: Long = 0,
    var enabled: Boolean = true,
    var bodyMatcher: BodyMatcherState = BodyMatcherState(),
    var priority: Int = 0
) {
    fun toMockRule(): MockRule = MockRule(
        id = id,
        path = path,
        method = HttpMethod.valueOf(method),
        statusCode = statusCode,
        headers = headers,
        body = body,
        delayMs = delayMs,
        enabled = enabled,
        bodyMatcher = bodyMatcher.toBodyMatcher(),
        priority = priority
    )

    companion object {
        fun fromMockRule(rule: MockRule): MockRuleState = MockRuleState(
            id = rule.id,
            path = rule.path,
            method = rule.method.name,
            statusCode = rule.statusCode,
            headers = rule.headers.toMutableMap(),
            body = rule.body,
            delayMs = rule.delayMs,
            enabled = rule.enabled,
            bodyMatcher = BodyMatcherState.fromBodyMatcher(rule.bodyMatcher),
            priority = rule.priority
        )
    }
}

/**
 * Service for managing the built-in Mock Server with persistent state.
 *
 * Provides a local HTTP server that can mock API responses based on configured rules.
 * Uses the JDK built-in HttpServer for lightweight operation.
 * Persists rules across IDE restarts.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "AmateurPostmanMockServer",
    storages = [Storage("amateur-postman-mock.xml")]
)
class MockServerManager : PersistentStateComponent<MockServerState> {

    private var state = MockServerState()
    private var server: HttpServer? = null
    private val rules = ConcurrentHashMap<String, MockRule>()
    private val logger = thisLogger()
    private val executor = Executors.newCachedThreadPool()

    /**
     * Current port the server is running on, or null if not running.
     */
    val runningPort: Int?
        get() = if (server != null) server?.address?.port else null

    /**
     * Whether the server is currently running.
     */
    val isRunning: Boolean
        get() = server != null

    /**
     * All registered mock rules.
     */
    val allRules: List<MockRule>
        get() = rules.values.toList()

    // ========== PersistentStateComponent Implementation ==========

    override fun getState(): MockServerState {
        // Sync in-memory rules to state
        state.rules.clear()
        state.rules.addAll(rules.values.map { MockRuleState.fromMockRule(it) })
        return state
    }

    override fun loadState(state: MockServerState) {
        this.state = state
        // Load persisted rules into memory
        rules.clear()
        state.rules.forEach { ruleState ->
            try {
                val rule = ruleState.toMockRule()
                rules[rule.id] = rule
            } catch (e: Exception) {
                logger.warn("Failed to load mock rule: ${ruleState.id}", e)
            }
        }
        logger.info("Loaded ${rules.size} mock rules from persistence")
    }

    // ========== Server Lifecycle ==========

    /**
     * Starts the mock server.
     *
     * @param port Port to bind to (0 = auto-select available port)
     * @return The actual port the server is running on, or null if failed to start
     */
    suspend fun start(port: Int = 0): Int? = withContext(Dispatchers.IO) {
        if (server != null) {
            logger.warn("Mock server is already running on port ${runningPort}")
            return@withContext runningPort
        }

        try {
            val httpServer = HttpServer.create(InetSocketAddress(port), 0)
            httpServer.createContext("/", MockHandler())
            httpServer.executor = executor // Use thread pool for concurrent request handling
            httpServer.start()

            server = httpServer
            val actualPort = httpServer.address.port
            logger.info("Mock server started on port $actualPort")
            actualPort
        } catch (e: Exception) {
            logger.error("Failed to start mock server", e)
            null
        }
    }

    /**
     * Stops the mock server.
     */
    fun stop() {
        server?.stop(0)
        server = null
        executor.shutdown()
        logger.info("Mock server stopped")
    }

    // ========== Rule Management ==========

    /**
     * Adds a mock rule.
     *
     * @param rule The rule to add
     * @return The added rule with generated ID if not provided
     */
    fun addRule(rule: MockRule): MockRule {
        rules[rule.id] = rule
        logger.info("Added mock rule: ${rule.method} ${rule.path}")
        return rule
    }

    /**
     * Removes a mock rule by ID.
     *
     * @param id The rule ID to remove
     * @return Whether the rule was removed
     */
    fun removeRule(id: String): Boolean {
        val removed = rules.remove(id) != null
        if (removed) {
            logger.info("Removed mock rule: $id")
        }
        return removed
    }

    /**
     * Gets a mock rule by ID.
     *
     * @param id The rule ID
     * @return The rule, or null if not found
     */
    fun getRule(id: String): MockRule? = rules[id]

    /**
     * Clears all mock rules.
     */
    fun clearRules() {
        rules.clear()
        logger.info("Cleared all mock rules")
    }

    /**
     * Finds a matching rule for the given request.
     * Rules are sorted by priority (descending), so higher priority rules are matched first.
     *
     * @param path Request path
     * @param method Request method
     * @param requestBody Optional request body for body matching
     * @return The matching rule with highest priority, or null if none found
     */
    fun findMatchingRule(path: String, method: HttpMethod, requestBody: String? = null): MockRule? {
        return rules.values
            .filter { it.enabled }
            .sortedByDescending { it.priority }
            .find { rule ->
                rule.path == path && rule.method == method && rule.bodyMatcher.matches(requestBody)
            }
    }

    // ========== HTTP Handler ==========

    /**
     * Handler for incoming HTTP requests.
     */
    private inner class MockHandler : com.sun.net.httpserver.HttpHandler {
        override fun handle(exchange: com.sun.net.httpserver.HttpExchange) {
            val path = exchange.requestURI.path
            val methodResult = parseHttpMethod(exchange.requestMethod)

            // Handle unknown method
            if (methodResult == null) {
                handleMethodNotAllowed(exchange, exchange.requestMethod)
                return
            }

            // Check Content-Length header for size limit
            val contentLength = exchange.requestHeaders.getFirst("Content-Length")?.toLongOrNull() ?: 0
            if (contentLength > MAX_REQUEST_BODY_SIZE) {
                handlePayloadTooLarge(exchange)
                return
            }

            // Read request body for body matching (with size limit)
            val requestBody = try {
                exchange.requestBody.use { input ->
                    // Read with limit to prevent OOM
                    val buffer = ByteArray(minOf(contentLength.toInt(), MAX_REQUEST_BODY_SIZE + 1))
                    val bytesRead = input.read(buffer)
                    if (bytesRead > MAX_REQUEST_BODY_SIZE) {
                        handlePayloadTooLarge(exchange)
                        return
                    }
                    if (bytesRead > 0) String(buffer, 0, bytesRead, Charsets.UTF_8) else null
                }
            } catch (e: Exception) {
                null
            }

            logger.debug("Received request: $methodResult $path (body: ${requestBody?.take(100)}...)")

            val rule = findMatchingRule(path, methodResult, requestBody)

            if (rule != null) {
                handleMockResponse(exchange, rule)
            } else {
                handleNoMatch(exchange, path, methodResult)
            }
        }

        private fun handleMockResponse(
            exchange: com.sun.net.httpserver.HttpExchange,
            rule: MockRule
        ) {
            // Apply delay if configured
            // Note: Thread.sleep is acceptable here because we use a cached thread pool
            // that can handle multiple concurrent requests without blocking each other.
            // Each delayed request only blocks its own handler thread.
            if (rule.delayMs > 0) {
                Thread.sleep(rule.delayMs)
            }

            // Set response headers
            rule.headers.forEach { (key, value) ->
                exchange.responseHeaders.add(key, value)
            }

            // Set content-type if not specified
            if (!rule.headers.keys.any { it.equals("Content-Type", ignoreCase = true) }) {
                exchange.responseHeaders.add("Content-Type", "application/json")
            }

            val responseBody = rule.body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(rule.statusCode, responseBody.size.toLong())
            exchange.responseBody.use { os ->
                os.write(responseBody)
            }

            logger.debug("Sent mock response: ${rule.statusCode} for ${rule.method} ${rule.path}")
        }

        private fun handleNoMatch(
            exchange: com.sun.net.httpserver.HttpExchange,
            path: String,
            method: HttpMethod
        ) {
            val response = """{"error":"No mock rule found","path":"$path","method":"$method"}"""
            val responseBody = response.toByteArray(Charsets.UTF_8)

            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(404, responseBody.size.toLong())
            exchange.responseBody.use { os ->
                os.write(responseBody)
            }

            logger.debug("No matching rule for $method $path")
        }

        private fun handleMethodNotAllowed(
            exchange: com.sun.net.httpserver.HttpExchange,
            method: String
        ) {
            val response = """{"error":"Method Not Allowed","method":"$method"}"""
            val responseBody = response.toByteArray(Charsets.UTF_8)

            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.responseHeaders.add("Allow", "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS")
            exchange.sendResponseHeaders(405, responseBody.size.toLong())
            exchange.responseBody.use { os ->
                os.write(responseBody)
            }

            logger.debug("Method not allowed: $method")
        }

        private fun handlePayloadTooLarge(exchange: com.sun.net.httpserver.HttpExchange) {
            val response = """{"error":"Payload Too Large","maxSize":$MAX_REQUEST_BODY_SIZE}"""
            val responseBody = response.toByteArray(Charsets.UTF_8)

            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(413, responseBody.size.toLong())
            exchange.responseBody.use { os ->
                os.write(responseBody)
            }

            logger.debug("Request payload too large")
        }

        /**
         * Parses HTTP method string to HttpMethod enum.
         * Returns null for unknown methods instead of defaulting to GET.
         */
        private fun parseHttpMethod(method: String): HttpMethod? {
            return try {
                HttpMethod.valueOf(method.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    companion object {
        /**
         * Maximum request body size in bytes (1MB).
         * Requests larger than this will be rejected with 413 Payload Too Large.
         */
        const val MAX_REQUEST_BODY_SIZE = 1024 * 1024 // 1MB

        fun getInstance(project: Project): MockServerManager {
            return project.service()
        }
    }
}
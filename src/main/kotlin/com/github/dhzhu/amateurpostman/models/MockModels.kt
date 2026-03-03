package com.github.dhzhu.amateurpostman.models

/**
 * Match mode for request body matching.
 */
enum class BodyMatchMode {
    NONE,       // Don't match request body
    EXACT,      // Exact string match
    CONTAINS,   // Contains substring
    REGEX       // Regular expression match
}

/**
 * Configuration for request body matching.
 *
 * @property mode Match mode
 * @property pattern Pattern to match (exact string, substring, or regex)
 */
data class BodyMatcher(
    val mode: BodyMatchMode = BodyMatchMode.NONE,
    val pattern: String = ""
) {
    /**
     * Tests if the given request body matches this matcher.
     */
    fun matches(requestBody: String?): Boolean {
        if (mode == BodyMatchMode.NONE) return true
        if (requestBody == null) return false

        return when (mode) {
            BodyMatchMode.NONE -> true
            BodyMatchMode.EXACT -> requestBody == pattern
            BodyMatchMode.CONTAINS -> requestBody.contains(pattern)
            BodyMatchMode.REGEX -> {
                try {
                    val regex = Regex(pattern, RegexOption.MULTILINE)
                    regex.containsMatchIn(requestBody)
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}

/**
 * Represents a mock rule for the built-in Mock Server.
 *
 * @property id Unique identifier for this rule
 * @property path URL path to match (e.g., "/api/users")
 * @property method HTTP method to match
 * @property statusCode HTTP status code to return
 * @property headers Response headers
 * @property body Response body
 * @property delayMs Optional delay in milliseconds to simulate latency
 * @property enabled Whether this rule is active
 * @property bodyMatcher Optional request body matching configuration
 * @property priority Rule priority (higher value = higher priority). Default is 0.
 */
data class MockRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val method: HttpMethod = HttpMethod.GET,
    val statusCode: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val delayMs: Long = 0,
    val enabled: Boolean = true,
    val bodyMatcher: BodyMatcher = BodyMatcher(),
    val priority: Int = 0
) {
    companion object {
        /**
         * Creates a simple mock rule with default values.
         */
        fun create(
            path: String,
            method: HttpMethod = HttpMethod.GET,
            body: String = "",
            statusCode: Int = 200
        ): MockRule = MockRule(
            path = path,
            method = method,
            body = body,
            statusCode = statusCode
        )
    }
}

/**
 * Configuration for the Mock Server.
 *
 * @property port Port number for the server (0 = auto-select available port)
 * @property autoStart Whether to start the server automatically when plugin loads
 */
data class MockServerConfig(
    val port: Int = 0,
    val autoStart: Boolean = false
)
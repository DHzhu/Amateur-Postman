package com.github.dhzhu.amateurpostman.models

/** Represents the type of HTTP request body content */
enum class BodyType(val displayName: String, val mimeType: String, val fileExtension: String) {
    TEXT("Text", "text/plain", "txt"),
    JSON("JSON", "application/json", "json"),
    XML("XML", "application/xml", "xml"),
    HTML("HTML", "text/html", "html"),
    JAVASCRIPT("JavaScript", "application/javascript", "js"),
    FORM_URLENCODED("Form URL-Encoded", "application/x-www-form-urlencoded", "txt"),
    MULTIPART("Multipart", "multipart/form-data", "txt"),
    GRAPHQL("GraphQL", "application/json", "graphql");

    companion object {
        fun fromMimeType(mimeType: String?): BodyType {
            return entries.find { it.mimeType.equals(mimeType, ignoreCase = true) } ?: JSON
        }
    }
}

// ============================================================
// Authentication Models
// ============================================================

/** OAuth 2.0 grant types supported by the authentication framework */
enum class OAuth2GrantType(val displayName: String) {
    AUTHORIZATION_CODE("Authorization Code"),
    CLIENT_CREDENTIALS("Client Credentials"),
    PASSWORD("Password"),
    IMPLICIT("Implicit")
}

/** Represents an OAuth 2.0 token with metadata */
data class OAuth2Token(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long? = null,
    val refreshToken: String? = null,
    val scope: String? = null,
    val createdAt: Long = System.currentTimeMillis() / 1000
) {
    /** Check if the token is expired (with 60 second buffer) */
    fun isExpired(): Boolean {
        if (expiresIn == null) return false
        val expiresAt = createdAt + expiresIn
        val now = System.currentTimeMillis() / 1000
        return now >= expiresAt - 60
    }

    /** Check if the token can be refreshed */
    fun canRefresh(): Boolean = !refreshToken.isNullOrBlank()
}

/** Configuration for OAuth 2.0 authentication */
data class OAuth2Config(
    val grantType: OAuth2GrantType,
    val authUrl: String? = null,          // Required for Authorization Code and Implicit
    val tokenUrl: String,                  // Token endpoint URL
    val clientId: String,
    val clientSecret: String? = null,      // Required for Client Credentials, optional for others
    val scope: String? = null,
    val username: String? = null,          // Required for Password grant
    val password: String? = null,          // Required for Password grant
    val redirectUri: String? = null,       // Required for Authorization Code
    val accessToken: OAuth2Token? = null   // Current token
)

/**
 * Sealed interface representing different authentication methods for HTTP requests.
 * Each implementation knows how to convert itself into HTTP headers.
 */
sealed interface Authentication {
    /** Convert authentication to HTTP headers map */
    fun toHeaders(): Map<String, String>
}

/** No authentication */
data object NoAuth : Authentication {
    override fun toHeaders(): Map<String, String> = emptyMap()
}

/** Basic Authentication using username and password */
data class BasicAuth(
    val username: String,
    val password: String
) : Authentication {
    override fun toHeaders(): Map<String, String> {
        val credentials = java.util.Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray())
        return mapOf("Authorization" to "Basic $credentials")
    }
}

/** Bearer Token Authentication */
data class BearerToken(
    val token: String
) : Authentication {
    override fun toHeaders(): Map<String, String> {
        return mapOf("Authorization" to "Bearer $token")
    }
}

/** API Key Authentication (header-based) */
data class ApiKeyAuth(
    val key: String,
    val value: String,
    val addTo: ApiKeyLocation = ApiKeyLocation.HEADER
) : Authentication {
    enum class ApiKeyLocation { HEADER, QUERY }

    override fun toHeaders(): Map<String, String> {
        return if (addTo == ApiKeyLocation.HEADER) {
            mapOf(key to value)
        } else {
            emptyMap() // Query params handled separately
        }
    }
}

/** OAuth 2.0 Authentication */
data class OAuth2Auth(
    val config: OAuth2Config,
    val token: OAuth2Token? = config.accessToken
) : Authentication {
    override fun toHeaders(): Map<String, String> {
        if (token == null || token.isExpired()) return emptyMap()
        return mapOf("Authorization" to "${token.tokenType} ${token.accessToken}")
    }
}

/** Represents a single part in a multipart/form-data request */
sealed class MultipartPart {
    data class TextField(
        val key: String,
        val value: String,
        val contentType: String? = null,
        val description: String = ""
    ) : MultipartPart()

    data class FileField(
        val key: String,
        val filePath: String,
        val fileName: String,
        val contentType: String? = null,
        val description: String = ""
    ) : MultipartPart() {
        val isEmpty: Boolean get() = filePath.isBlank()
    }
}

/** Represents a GraphQL request with query and optional variables */
data class GraphQLRequest(
    val query: String,
    val operationName: String? = null,
    val variables: String? = null
) {
    /**
     * Converts GraphQL request to standard JSON format for HTTP body.
     * Returns a JSON string like: {"query":"...","variables":{...}}
     */
    fun toJson(): String {
        val gson = com.google.gson.Gson()
        val map = mutableMapOf<String, Any?>("query" to query)
        if (operationName != null) {
            map["operationName"] = operationName
        }
        if (variables != null && variables.isNotBlank()) {
            try {
                // Parse variables as JSON to validate it
                val varsJson = com.google.gson.JsonParser.parseString(variables)
                map["variables"] = varsJson
            } catch (e: Exception) {
                // If variables is not valid JSON, include as string
                map["variables"] = variables
            }
        }
        return gson.toJson(map)
    }

    companion object {
        /**
         * Parses a JSON body into GraphQLRequest components.
         * Expected JSON format: {"query":"...","variables":{...},"operationName":"..."}
         */
        fun fromJson(jsonBody: String): GraphQLRequest? {
            return try {
                val element = com.google.gson.JsonParser.parseString(jsonBody)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val query = obj.get("query")?.asString ?: return null
                    val operationName = obj.get("operationName")?.asString
                    val variables = obj.get("variables")?.toString()
                    GraphQLRequest(query, operationName, variables)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

/** Represents the HTTP request body with type information */
data class HttpBody(
    val content: String,
    val type: BodyType = BodyType.JSON,
    val multipartData: List<MultipartPart>? = null
) {
    val isEmpty: Boolean get() = content.isBlank() && (multipartData?.isEmpty() != false)

    companion object {
        val Empty = HttpBody("", BodyType.JSON)

        /**
         * Factory method for creating HttpBody with content and type.
         * Provides binary compatibility for code expecting the old 2-argument constructor.
         */
        @JvmStatic
        fun of(content: String, type: BodyType): HttpBody = HttpBody(content, type, null)

        /**
         * Factory method for creating HttpBody with content only (defaults to JSON type).
         */
        @JvmStatic
        fun of(content: String): HttpBody = HttpBody(content, BodyType.JSON, null)
    }
}

/** Represents an HTTP request to be executed */
data class HttpRequest(
        val url: String,
        val method: HttpMethod,
        val headers: Map<String, String> = emptyMap(),
        val body: HttpBody? = null,
        val authentication: Authentication? = null
) {
    /** @deprecated Use body.type.mimeType instead */
    val contentType: String? get() = body?.type?.mimeType

    /**
     * Returns headers merged with authentication headers.
     * Authentication headers take precedence over existing headers with the same key.
     */
    fun getEffectiveHeaders(): Map<String, String> {
        val authHeaders = authentication?.toHeaders() ?: emptyMap()
        return headers + authHeaders
    }
}

/** Network timing data for HTTP profiling */
data class HttpProfilingData(
    /** DNS resolution duration in milliseconds (null for connection reuse) */
    val dnsDuration: Long?,
    /** TCP connection duration in milliseconds (null for connection reuse) */
    val tcpDuration: Long?,
    /** SSL/TLS handshake duration in milliseconds (null for connection reuse or no SSL) */
    val sslDuration: Long?,
    /** Time to First Byte in milliseconds */
    val ttfbDuration: Long?,
    /** Total request duration in milliseconds */
    val totalDuration: Long,
    /** Whether the connection was reused from the pool */
    val connectionReused: Boolean
) {
    companion object {
        /** Empty profiling data when no timing information is available */
        val Empty = HttpProfilingData(
            dnsDuration = null,
            tcpDuration = null,
            sslDuration = null,
            ttfbDuration = null,
            totalDuration = 0,
            connectionReused = false
        )
    }
}

/** Represents an HTTP response received from the server */
data class HttpResponse(
        val statusCode: Int,
        val statusMessage: String,
        val headers: Map<String, List<String>>,
        val body: String,
        val duration: Long, // Duration in milliseconds
        val isSuccessful: Boolean = statusCode in 200..299,
        val profilingData: HttpProfilingData = HttpProfilingData.Empty
)

/** Supported HTTP methods */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS;

    override fun toString(): String = name
}

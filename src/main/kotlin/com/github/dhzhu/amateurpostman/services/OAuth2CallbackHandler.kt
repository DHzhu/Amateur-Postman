package com.github.dhzhu.amateurpostman.services

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.net.URI
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Result of an authorization code callback.
 */
sealed class AuthCallbackResult {
    data class Success(val code: String, val state: String) : AuthCallbackResult()
    data class Error(val error: String, val description: String?) : AuthCallbackResult()
    data class Timeout(val message: String = "Authorization timed out") : AuthCallbackResult()
}

/**
 * Handles OAuth 2.0 browser-based authorization flows.
 * Starts a local HTTP server to receive the callback from the authorization server.
 */
class OAuth2CallbackServer(
    private val port: Int = 0, // 0 means auto-assign
    private val callbackPath: String = "/callback",
    private val timeoutSeconds: Long = 300 // 5 minutes default
) {
    private val logger = thisLogger()
    private var server: com.sun.net.httpserver.HttpServer? = null
    private val callbackLatch = CountDownLatch(1)
    private val callbackResult = AtomicReference<AuthCallbackResult>()

    /**
     * Gets the actual port the server is listening on.
     * Only valid after start() is called.
     */
    val actualPort: Int
        get() = server?.address?.port ?: 0

    /**
     * Gets the redirect URI that should be used in the authorization request.
     */
    val redirectUri: String
        get() = "http://localhost:$actualPort$callbackPath"

    /**
     * Starts the callback server.
     */
    fun start(): Boolean {
        return try {
            server = com.sun.net.httpserver.HttpServer.create(InetSocketAddress(port), 0).apply {
                createContext(callbackPath) { exchange ->
                    handleCallback(exchange)
                }
                start()
            }
            logger.info("OAuth callback server started on port $actualPort")
            true
        } catch (e: Exception) {
            logger.error("Failed to start OAuth callback server", e)
            false
        }
    }

    /**
     * Stops the callback server.
     */
    fun stop() {
        server?.stop(1)
        server = null
        logger.info("OAuth callback server stopped")
    }

    /**
     * Waits for the authorization callback.
     * Returns the result of the callback.
     */
    suspend fun waitForCallback(): AuthCallbackResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutSeconds * 1000) {
                val reached = callbackLatch.await(timeoutSeconds, TimeUnit.SECONDS)
                if (!reached) {
                    AuthCallbackResult.Timeout()
                } else {
                    callbackResult.get()
                }
            }
        } catch (e: Exception) {
            logger.error("Error waiting for OAuth callback", e)
            AuthCallbackResult.Timeout("Authorization timed out: ${e.message}")
        }
    }

    private fun handleCallback(exchange: com.sun.net.httpserver.HttpExchange) {
        try {
            val query = exchange.requestURI.query ?: ""
            val params = parseQueryParams(query)

            val result = when {
                params.containsKey("error") -> {
                    AuthCallbackResult.Error(
                        error = params["error"] ?: "unknown_error",
                        description = params["error_description"]
                    )
                }
                params.containsKey("code") -> {
                    AuthCallbackResult.Success(
                        code = params["code"]!!,
                        state = params["state"] ?: ""
                    )
                }
                else -> {
                    AuthCallbackResult.Error(
                        error = "invalid_request",
                        description = "No authorization code in callback"
                    )
                }
            }

            callbackResult.set(result)
            callbackLatch.countDown()

            // Send response to browser
            val response = when (result) {
                is AuthCallbackResult.Success -> {
                    """<!DOCTYPE html>
                    <html>
                    <head><title>Authorization Successful</title></head>
                    <body>
                        <h1>Authorization Successful</h1>
                        <p>You can close this window now.</p>
                        <script>setTimeout(function() { window.close(); }, 2000);</script>
                    </body>
                    </html>"""
                }
                is AuthCallbackResult.Error -> {
                    """<!DOCTYPE html>
                    <html>
                    <head><title>Authorization Failed</title></head>
                    <body>
                        <h1>Authorization Failed</h1>
                        <p>Error: ${result.error}</p>
                        <p>${result.description ?: ""}</p>
                    </body>
                    </html>"""
                }
                is AuthCallbackResult.Timeout -> {
                    """<!DOCTYPE html>
                    <html>
                    <head><title>Authorization Timed Out</title></head>
                    <body>
                        <h1>Authorization Timed Out</h1>
                        <p>Please try again.</p>
                    </body>
                    </html>"""
                }
            }

            val responseBytes = response.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, responseBytes.size.toLong())
            exchange.responseBody.use { os ->
                os.write(responseBytes)
            }
        } catch (e: Exception) {
            logger.error("Error handling OAuth callback", e)
        } finally {
            exchange.close()
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        return query.split("&")
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1)?.let { v -> java.net.URLDecoder.decode(v, "UTF-8") } ?: "")
            }
    }
}

/**
 * Utility class for OAuth 2.0 authorization URL generation.
 */
object OAuth2AuthorizationUrl {

    /**
     * Generates an authorization URL for Authorization Code flow.
     */
    fun generateAuthCodeUrl(
        authUrl: String,
        clientId: String,
        redirectUri: String,
        scope: String? = null,
        state: String = UUID.randomUUID().toString(),
        additionalParams: Map<String, String> = emptyMap()
    ): String {
        val params = mutableMapOf(
            "response_type" to "code",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "state" to state
        )

        scope?.let { params["scope"] = it }
        params.putAll(additionalParams)

        return buildUrl(authUrl, params)
    }

    /**
     * Generates an authorization URL for Implicit flow.
     */
    fun generateImplicitUrl(
        authUrl: String,
        clientId: String,
        redirectUri: String,
        scope: String? = null,
        state: String = UUID.randomUUID().toString(),
        additionalParams: Map<String, String> = emptyMap()
    ): String {
        val params = mutableMapOf(
            "response_type" to "token",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "state" to state
        )

        scope?.let { params["scope"] = it }
        params.putAll(additionalParams)

        return buildUrl(authUrl, params)
    }

    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        val uri = URI(baseUrl)
        val existingQuery = uri.query
        val newQuery = params.entries.joinToString("&") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }

        val finalQuery = if (existingQuery.isNullOrBlank()) {
            newQuery
        } else {
            "$existingQuery&$newQuery"
        }

        return URI(
            uri.scheme,
            uri.authority,
            uri.path,
            finalQuery,
            uri.fragment
        ).toString()
    }
}

/**
 * Parses tokens from OAuth 2.0 callback URLs (for Implicit flow).
 */
object OAuth2TokenParser {

    /**
     * Parses an access token from a callback URL fragment.
     * Used for Implicit flow where the token is in the URL fragment (#access_token=...).
     */
    fun parseFromFragment(fragment: String): Map<String, String> {
        if (fragment.isBlank()) return emptyMap()

        // Remove leading # if present
        val cleanFragment = if (fragment.startsWith("#")) fragment.substring(1) else fragment

        return cleanFragment.split("&")
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1)?.let { v ->
                    java.net.URLDecoder.decode(v, "UTF-8")
                } ?: "")
            }
    }

    /**
     * Extracts the access token from a callback URL (for Implicit flow).
     */
    fun extractAccessToken(fragment: String): String? {
        return parseFromFragment(fragment)["access_token"]
    }

    /**
     * Extracts error information from a callback URL fragment.
     */
    fun extractError(fragment: String): Pair<String, String?>? {
        val params = parseFromFragment(fragment)
        val error = params["error"] ?: return null
        return error to params["error_description"]
    }
}
package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest

/** Exporter for converting HttpRequest objects to cURL commands */
object CurlExporter {

    /**
     * Export an HttpRequest to a cURL command string
     * @param request The HTTP request to export
     * @param multiLine If true, format the command across multiple lines for readability
     * @return The cURL command string
     */
    fun export(request: HttpRequest, multiLine: Boolean = false): String {
        val parts = mutableListOf<String>()
        val separator = if (multiLine) " \\\n  " else " "

        parts.add("curl")

        // Add method if not GET
        if (request.method != HttpMethod.GET) {
            parts.add("-X ${request.method}")
        }

        // Add headers
        request.headers.forEach { (key, value) ->
            val escapedValue = escapeForShell(value)
            parts.add("-H '${escapeForShell(key)}: $escapedValue'")
        }

        // Add body
        if (!request.body?.content.isNullOrEmpty()) {
            val escapedBody = escapeForShell(request.body?.content ?: "")
            parts.add("-d '$escapedBody'")
        }

        // Add URL
        parts.add("'${escapeForShell(request.url)}'")

        return parts.joinToString(separator)
    }

    /**
     * Export an HttpRequest to a cURL command, including common options
     * @param request The HTTP request to export
     * @param followRedirects If true, add -L flag
     * @param verbose If true, add -v flag
     * @param includeHeaders If true, add -i flag to show response headers
     * @return The cURL command string
     */
    fun exportWithOptions(
            request: HttpRequest,
            followRedirects: Boolean = true,
            verbose: Boolean = false,
            includeHeaders: Boolean = false,
            multiLine: Boolean = false
    ): String {
        val parts = mutableListOf<String>()
        val separator = if (multiLine) " \\\n  " else " "

        parts.add("curl")

        // Add options
        if (followRedirects) {
            parts.add("-L")
        }
        if (verbose) {
            parts.add("-v")
        }
        if (includeHeaders) {
            parts.add("-i")
        }

        // Add method if not GET
        if (request.method != HttpMethod.GET) {
            parts.add("-X ${request.method}")
        }

        // Add headers
        request.headers.forEach { (key, value) ->
            val escapedValue = escapeForShell(value)
            parts.add("-H '${escapeForShell(key)}: $escapedValue'")
        }

        // Add content type if specified and not already in headers
        if (request.body?.type?.mimeType != null && !request.headers.containsKey("Content-Type")) {
            parts.add("-H 'Content-Type: ${escapeForShell(request.body?.type?.mimeType ?: "")}'")
        }

        // Add body
        if (!request.body?.content.isNullOrEmpty()) {
            val escapedBody = escapeForShell(request.body?.content ?: "")
            parts.add("-d '$escapedBody'")
        }

        // Add URL
        parts.add("'${escapeForShell(request.url)}'")

        return parts.joinToString(separator)
    }

    /** Escape a string for use in a shell single-quoted string */
    private fun escapeForShell(str: String): String {
        // In single quotes, only single quote needs to be escaped
        // We do this by ending the single quote, adding an escaped single quote, and starting a new
        // single quote
        return str.replace("'", "'\\''")
    }

    /**
     * Format a cURL command for display with syntax highlighting markers
     * @param curlCommand The cURL command to format
     * @return A list of pairs where each pair is (text, type) for highlighting
     */
    fun formatForDisplay(curlCommand: String): List<Pair<String, TokenType>> {
        val result = mutableListOf<Pair<String, TokenType>>()
        val tokens = curlCommand.split(Regex("(?<=\\s)|(?=\\s)"))

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.isBlank() -> {
                    result.add(token to TokenType.WHITESPACE)
                }
                token == "curl" -> {
                    result.add(token to TokenType.COMMAND)
                }
                token.startsWith("-") -> {
                    result.add(token to TokenType.OPTION)
                }
                token.startsWith("'") || token.startsWith("\"") -> {
                    result.add(token to TokenType.STRING)
                }
                token.startsWith("http") -> {
                    result.add(token to TokenType.URL)
                }
                else -> {
                    result.add(token to TokenType.TEXT)
                }
            }
            i++
        }

        return result
    }

    enum class TokenType {
        COMMAND,
        OPTION,
        STRING,
        URL,
        WHITESPACE,
        TEXT
    }
}

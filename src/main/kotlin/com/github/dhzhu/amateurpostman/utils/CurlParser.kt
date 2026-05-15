package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.BodyType
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest

/** Parser for cURL commands to convert them into HttpRequest objects */
object CurlParser {

    /**
     * Parse a cURL command string into an HttpRequest object
     * @param curlCommand The cURL command to parse
     * @return HttpRequest parsed from the cURL command
     * @throws IllegalArgumentException if the command is invalid
     */
    fun parse(curlCommand: String): HttpRequest {
        val trimmed = curlCommand.trim()

        require(trimmed.startsWith("curl", ignoreCase = true)) { "Command must start with 'curl'" }

        var url = ""
        var method = HttpMethod.GET
        val headers = mutableMapOf<String, String>()
        var body: String? = null
        var contentType: String? = null

        val tokens = tokenize(trimmed)
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.equals("curl", ignoreCase = true) -> {
                    // Skip the curl command itself
                    i++
                }
                token == "-X" || token == "--request" -> {
                    i++
                    if (i < tokens.size) {
                        method = parseMethod(tokens[i])
                        i++
                    }
                }
                token == "-H" || token == "--header" -> {
                    i++
                    if (i < tokens.size) {
                        val header = parseHeader(tokens[i])
                        if (header != null) {
                            headers[header.first] = header.second
                            if (header.first.equals("Content-Type", ignoreCase = true)) {
                                contentType = header.second
                            }
                        }
                        i++
                    }
                }
                token == "-d" ||
                        token == "--data" ||
                        token == "--data-raw" ||
                        token == "--data-binary" -> {
                    i++
                    if (i < tokens.size) {
                        body = tokens[i]
                        // If body is set without explicit method, default to POST
                        if (method == HttpMethod.GET) {
                            method = HttpMethod.POST
                        }
                        i++
                    }
                }
                token == "-u" || token == "--user" -> {
                    i++
                    if (i < tokens.size) {
                        val credentials = tokens[i]
                        val encoded =
                                java.util.Base64.getEncoder()
                                        .encodeToString(credentials.toByteArray())
                        headers["Authorization"] = "Basic $encoded"
                        i++
                    }
                }
                token == "-A" || token == "--user-agent" -> {
                    i++
                    if (i < tokens.size) {
                        headers["User-Agent"] = tokens[i]
                        i++
                    }
                }
                token == "-L" || token == "--location" -> {
                    // Follow redirects - already default behavior in OkHttp
                    i++
                }
                token == "-k" || token == "--insecure" -> {
                    // Skip SSL verification - not supported currently
                    i++
                }
                token.startsWith("-") -> {
                    // Unknown option, skip
                    i++
                    // Check if next token is a value for this option
                    if (i < tokens.size && !tokens[i].startsWith("-") && !isUrl(tokens[i])) {
                        i++
                    }
                }
                isUrl(token) -> {
                    url = token.removeSurrounding("'").removeSurrounding("\"")
                    i++
                }
                else -> {
                    // Could be the URL without protocol
                    if (url.isEmpty() && !token.startsWith("-")) {
                        url = token.removeSurrounding("'").removeSurrounding("\"")
                    }
                    i++
                }
            }
        }

        require(url.isNotEmpty()) { "No URL found in cURL command" }

        // Determine body type from content type
        val bodyType = BodyType.fromMimeType(contentType ?: headers["Content-Type"])
        val requestBody = body?.let { HttpBody(it, bodyType) }

        return HttpRequest(
                url = url,
                method = method,
                headers = headers,
                body = requestBody
        )
    }

    /** Tokenize a cURL command, handling quoted strings */
    private fun tokenize(command: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false
        var i = 0

        while (i < command.length) {
            val c = command[i]
            when {
                c == '\'' && !inDoubleQuote -> {
                    if (inSingleQuote) {
                        // End of single-quoted string
                        tokens.add(current.toString())
                        current = StringBuilder()
                        inSingleQuote = false
                    } else {
                        // Start of single-quoted string
                        if (current.isNotEmpty()) {
                            tokens.add(current.toString())
                            current = StringBuilder()
                        }
                        inSingleQuote = true
                    }
                    i++
                }
                c == '"' && !inSingleQuote -> {
                    if (inDoubleQuote) {
                        // End of double-quoted string
                        tokens.add(current.toString())
                        current = StringBuilder()
                        inDoubleQuote = false
                    } else {
                        // Start of double-quoted string
                        if (current.isNotEmpty()) {
                            tokens.add(current.toString())
                            current = StringBuilder()
                        }
                        inDoubleQuote = true
                    }
                    i++
                }
                c == '\\' && i + 1 < command.length -> {
                    // Handle escape sequences
                    val next = command[i + 1]
                    if (next == '\n' || next == '\r') {
                        // Line continuation, skip
                        i += 2
                        if (i < command.length && command[i] == '\n') {
                            i++
                        }
                    } else if (inDoubleQuote || inSingleQuote) {
                        current.append(next)
                        i += 2
                    } else {
                        current.append(next)
                        i += 2
                    }
                }
                c.isWhitespace() && !inSingleQuote && !inDoubleQuote -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current = StringBuilder()
                    }
                    i++
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        return tokens
    }

    private fun parseMethod(methodStr: String): HttpMethod {
        return try {
            HttpMethod.valueOf(methodStr.uppercase())
        } catch (e: IllegalArgumentException) {
            HttpMethod.GET
        }
    }

    private fun parseHeader(headerStr: String): Pair<String, String>? {
        val colonIndex = headerStr.indexOf(':')
        if (colonIndex <= 0) return null

        val key = headerStr.substring(0, colonIndex).trim()
        val value = headerStr.substring(colonIndex + 1).trim()
        return key to value
    }

    private fun isUrl(str: String): Boolean {
        val cleaned = str.removeSurrounding("'").removeSurrounding("\"")
        return cleaned.startsWith("http://") ||
                cleaned.startsWith("https://") ||
                cleaned.startsWith("www.")
    }
}

package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.Variable
import com.intellij.openapi.diagnostic.thisLogger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.random.Random

/**
 * Utility for variable substitution in HTTP requests.
 *
 * Supports:
 * - Variable syntax: `{{variableName}}`
 * - Built-in functions: `{{$timestamp}}`, `{{$uuid}}`, `{{$randomInt}}`, etc.
 * - Recursive variable resolution (max 10 passes)
 * - Case-insensitive variable lookup
 */
object VariableResolver {

    private val logger = thisLogger()
    private const val MAX_RECURSION_PASSES = 10
    private const val VARIABLE_PATTERN = """\{\{([^}]+)}}"""
    private val variableRegex = VARIABLE_PATTERN.toRegex()

    /**
     * Substitutes variables in an HTTP request.
     *
     * @param request The HTTP request with potential variables
     * @param variables Map of variable names to values
     * @return HttpRequest with all variables substituted
     */
    fun substitute(request: HttpRequest, variables: Map<String, String>): HttpRequest {
        if (variables.isEmpty()) {
            return request
        }

        val substitutedUrl = substituteVariables(request.url, variables)
        val substitutedHeaders = request.headers.mapValues { (_, value) ->
            substituteVariables(value, variables)
        }
        val substitutedBody = request.body?.let { body ->
            body.copy(content = substituteVariables(body.content, variables))
        }

        return request.copy(
            url = substitutedUrl,
            headers = substitutedHeaders,
            body = substitutedBody
        )
        // Note: authentication is preserved through copy() as it's not modified
    }

    /**
     * Substitutes variables in a string.
     * Handles both regular variables and built-in functions.
     * Supports recursive resolution (variables referencing other variables).
     *
     * @param text The text containing variables
     * @param variables Map of variable names to values
     * @param recursionDepth Current recursion depth (internal use)
     * @return String with all variables substituted
     */
    fun substituteVariables(
        text: String,
        variables: Map<String, String>,
        recursionDepth: Int = 0
    ): String {
        if (recursionDepth >= MAX_RECURSION_PASSES) {
            logger.warn("Max recursion depth ($MAX_RECURSION_PASSES) reached while substituting variables in: $text")
            return text
        }

        val result = singlePassSubstitute(text, variables)

        return if (result !== text && variableRegex.containsMatchIn(result)) {
            substituteVariables(result, variables, recursionDepth + 1)
        } else {
            result
        }
    }

    private fun singlePassSubstitute(text: String, variables: Map<String, String>): String {
        val sb = StringBuilder(text.length)
        var lastEnd = 0
        var hasSubstitution = false

        variableRegex.findAll(text).forEach { match ->
            val content = match.groupValues[1].trim()
            val replacement = when {
                content.startsWith("$") -> resolveBuiltinFunction(content)
                content.isNotEmpty() -> variables[content.lowercase()]
                else -> null
            }

            if (replacement != null) {
                sb.append(text, lastEnd, match.range.first)
                sb.append(replacement)
                lastEnd = match.range.last + 1
                hasSubstitution = true
            } else {
                logger.debug("Variable not found: $content")
            }
        }

        if (!hasSubstitution) return text
        if (lastEnd < text.length) sb.append(text, lastEnd, text.length)
        return sb.toString()
    }

    /**
     * Resolves built-in functions.
     *
     * @param function The function string (e.g., "$timestamp", "$randomInt:1,100")
     * @return The resolved value, or null if function is unknown
     */
    private fun resolveBuiltinFunction(function: String): String? {
        return when {
            // Timestamp functions
            function == "\$timestamp" -> System.currentTimeMillis().toString()
            function.startsWith("\$timestamp:") -> {
                val format = function.substringAfter(":")
                formatTimestamp(format)
            }

            // UUID/GUID functions
            function == "\$uuid" -> UUID.randomUUID().toString()
            function == "\$guid" -> UUID.randomUUID().toString().replace("-", "")

            // Random integer functions
            function == "\$randomInt" -> Random.nextInt().toString()
            function.startsWith("\$randomInt:") -> {
                val params = function.substringAfter(":")
                resolveRandomInt(params)
            }

            // Random string function
            function.startsWith("\$randomString:") -> {
                val length = function.substringAfter(":").toIntOrNull() ?: 10
                generateRandomString(length)
            }

            // Unknown function
            else -> {
                logger.warn("Unknown built-in function: $function")
                null
            }
        }
    }

    /**
     * Formats timestamp using SimpleDateFormat.
     *
     * @param format The date format pattern
     * @return Formatted timestamp
     */
    private fun formatTimestamp(format: String): String {
        return try {
            val sdf = SimpleDateFormat(format)
            sdf.format(Date())
        } catch (e: Exception) {
            logger.warn("Invalid timestamp format: $format, using default")
            System.currentTimeMillis().toString()
        }
    }

    /**
     * Resolves random integer with parameters.
     * Supports:
     * - `$randomInt:min,max` - Random between min and max
     * - `$randomInt:length` - Random integer with specified digits
     *
     * @param params The parameters (either "min,max" or "length")
     * @return Random integer as string
     */
    private fun resolveRandomInt(params: String): String {
        return if (params.contains(",")) {
            // Range format: min,max
            val parts = params.split(",")
            if (parts.size == 2) {
                val min = parts[0].toIntOrNull() ?: 0
                val max = parts[1].toIntOrNull() ?: Int.MAX_VALUE
                Random.nextInt(min, max + 1).toString()
            } else {
                Random.nextInt().toString()
            }
        } else {
            // Length format: length
            val length = params.toIntOrNull() ?: 1
            if (length <= 0) {
                return "0"
            }
            val max = 10.0.pow(length.toDouble()).toInt() - 1
            val min = 10.0.pow((length - 1).toDouble()).toInt()
            if (length == 1) {
                Random.nextInt(0, 10).toString()
            } else {
                Random.nextInt(min, max + 1).toString()
            }
        }
    }

    /**
     * Generates a random alphanumeric string.
     *
     * @param length The length of the string to generate
     * @return Random alphanumeric string
     */
    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Extracts all variable names from text.
     * Returns both regular variables and built-in function names.
     *
     * @param text The text to extract variables from
     * @return Set of variable names (without {{ }})
     */
    fun extractVariableNames(text: String): Set<String> {
        return variableRegex.findAll(text)
            .map { it.groupValues[1].trim() }
            .toSet()
    }

    /**
     * Validates that all variables in a request exist in the provided variables map.
     * Built-in functions are always considered valid.
     *
     * @param request The HTTP request to validate
     * @param variables Map of available variables
     * @return List of missing variable names (empty if all valid)
     */
    fun validateVariables(
        request: HttpRequest,
        variables: Map<String, String>
    ): List<String> {
        val allText = buildString {
            append(request.url)
            append("\n")
            request.headers.values.forEach { append(it).append("\n") }
            request.body?.let { append(it.content) }
        }

        val variableNames = extractVariableNames(allText)
        val missing = mutableListOf<String>()

        variableNames.forEach { name ->
            if (name.startsWith("$")) {
                // Built-in functions are always valid
                return@forEach
            }

            val normalizedName = name.lowercase()
            if (!variables.containsKey(normalizedName)) {
                missing.add(name)
            }
        }

        return missing
    }

    /**
     * Gets the value of a variable from an environment.
     * Returns null if variable is not found or is disabled.
     *
     * @param variables List of variables
     * @param key The variable key (case-insensitive)
     * @return The variable value, or null if not found or disabled
     */
    fun getVariableValue(variables: List<Variable>, key: String): String? {
        val normalizedKey = key.lowercase()
        return variables
            .firstOrNull { it.key.lowercase() == normalizedKey && it.enabled }
            ?.value
    }

    /**
     * Converts a list of Variable objects to a Map.
     * Only includes enabled variables.
     * Keys are normalized to lowercase for case-insensitive lookup.
     *
     * @param variables List of variables
     * @return Map of normalized keys to values
     */
    fun variablesToMap(variables: List<Variable>): Map<String, String> {
        return variables
            .filter { it.enabled }
            .associate { it.key.lowercase() to it.value }
    }
}

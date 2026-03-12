package com.github.dhzhu.amateurpostman.utils

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import java.io.File
import java.io.IOException

/**
 * Parser for OpenAPI specifications (Swagger 2.0 and OpenAPI 3.x).
 *
 * Uses swagger-parser library to parse OpenAPI documents from various sources
 * (URL, file, or raw content) in JSON or YAML format.
 */
object OpenApiParser {

    /**
     * Result of parsing an OpenAPI specification.
     *
     * @property openAPI The parsed OpenAPI object, null if parsing failed
     * @property messages Validation messages (errors and warnings)
     * @property error Critical error message if parsing failed completely
     */
    data class ParseResult(
        val openAPI: OpenAPI?,
        val messages: List<String>,
        val error: String? = null
    ) {
        val isSuccess: Boolean get() = openAPI != null
        val hasWarnings: Boolean get() = messages.isNotEmpty() && error == null

        companion object {
            fun success(openAPI: OpenAPI, messages: List<String> = emptyList()) =
                ParseResult(openAPI, messages, null)

            fun error(message: String, messages: List<String> = emptyList()) =
                ParseResult(null, messages, message)
        }
    }

    /**
     * Parses an OpenAPI specification from a URL.
     *
     * @param url The URL to the OpenAPI specification (JSON or YAML)
     * @param resolveFully Whether to resolve all $ref references
     * @return ParseResult containing the parsed OpenAPI or error details
     */
    fun parseFromUrl(url: String, resolveFully: Boolean = true): ParseResult {
        return try {
            val options = createParseOptions(resolveFully)
            val result = OpenAPIV3Parser().readLocation(url, null, options)
            handleParseResult(result)
        } catch (e: Exception) {
            ParseResult.error("Failed to parse OpenAPI from URL: ${e.message}")
        }
    }

    /**
     * Parses an OpenAPI specification from a local file.
     *
     * @param file The file containing the OpenAPI specification
     * @param resolveFully Whether to resolve all $ref references
     * @return ParseResult containing the parsed OpenAPI or error details
     */
    fun parseFromFile(file: File, resolveFully: Boolean = true): ParseResult {
        return try {
            if (!file.exists()) {
                return ParseResult.error("File not found: ${file.absolutePath}")
            }
            val content = file.readText()
            parseFromContent(content, resolveFully)
        } catch (e: IOException) {
            ParseResult.error("Failed to read file: ${e.message}")
        } catch (e: Exception) {
            ParseResult.error("Failed to parse OpenAPI from file: ${e.message}")
        }
    }

    /**
     * Parses an OpenAPI specification from raw content string.
     *
     * @param content The raw OpenAPI specification content (JSON or YAML)
     * @param resolveFully Whether to resolve all $ref references
     * @return ParseResult containing the parsed OpenAPI or error details
     */
    fun parseFromContent(content: String, resolveFully: Boolean = true): ParseResult {
        return try {
            val options = createParseOptions(resolveFully)
            val result = OpenAPIV3Parser().readContents(content, null, options)
            handleParseResult(result)
        } catch (e: Exception) {
            ParseResult.error("Failed to parse OpenAPI content: ${e.message}")
        }
    }

    /**
     * Validates if a file is a valid OpenAPI specification.
     *
     * @param file The file to validate
     * @return true if valid OpenAPI, false otherwise
     */
    fun isValidOpenApi(file: File): Boolean {
        return try {
            val result = parseFromFile(file, resolveFully = false)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates if content is a valid OpenAPI specification.
     *
     * @param content The content to validate
     * @return true if valid OpenAPI, false otherwise
     */
    fun isValidOpenApi(content: String): Boolean {
        return try {
            val result = parseFromContent(content, resolveFully = false)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    private fun createParseOptions(resolveFully: Boolean): ParseOptions {
        return ParseOptions().apply {
            setResolveFully(resolveFully)
            setResolveCombinators(false)
        }
    }

    private fun handleParseResult(result: SwaggerParseResult): ParseResult {
        val messages = result.messages ?: emptyList()

        return if (result.openAPI != null) {
            if (messages.isNotEmpty()) {
                ParseResult.success(result.openAPI, messages)
            } else {
                ParseResult.success(result.openAPI)
            }
        } else {
            val errorMessage = if (messages.isNotEmpty()) {
                messages.joinToString("; ")
            } else {
                "Unknown parsing error"
            }
            ParseResult.error(errorMessage, messages)
        }
    }
}
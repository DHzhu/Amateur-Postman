package com.github.dhzhu.amateurpostman.utils

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.github.dhzhu.amateurpostman.services.JsonService
import java.io.File
import java.io.IOException

/**
 * Parser for HAR (HTTP Archive) 1.2 format files.
 *
 * Supports .har files exported from Chrome DevTools, Firefox, Fiddler,
 * and other network recording tools.
 *
 * HAR 1.2 spec: http://www.softwareishard.com/blog/har-12-spec/
 */
object HarParser {

    // ── HAR 1.2 data classes ─────────────────────────────────────────────────

    data class HarFile(val log: HarLog)

    data class HarLog(
        val version: String = "1.2",
        val creator: HarCreator = HarCreator(),
        val entries: List<HarEntry> = emptyList()
    )

    data class HarCreator(
        val name: String = "",
        val version: String = ""
    )

    data class HarEntry(
        val startedDateTime: String = "",
        val time: Double = 0.0,
        val request: HarRequest,
        val response: HarResponse = HarResponse(),
        /** Chrome DevTools extension field — identifies resource type */
        @JsonProperty("_resourceType") val resourceType: String? = null
    )

    data class HarRequest(
        val method: String,
        val url: String,
        val httpVersion: String = "HTTP/1.1",
        val headers: List<HarNameValue> = emptyList(),
        val queryString: List<HarNameValue> = emptyList(),
        val postData: HarPostData? = null,
        val headersSize: Int = -1,
        val bodySize: Int = -1
    )

    data class HarResponse(
        val status: Int = 0,
        val statusText: String = "",
        val headers: List<HarNameValue> = emptyList(),
        val content: HarContent = HarContent()
    )

    data class HarContent(
        val size: Long = 0,
        val mimeType: String = "",
        val text: String? = null,
        val encoding: String? = null
    )

    data class HarNameValue(
        val name: String,
        val value: String = ""
    )

    data class HarPostData(
        val mimeType: String = "",
        val text: String? = null,
        val params: List<HarParam>? = null
    )

    data class HarParam(
        val name: String,
        val value: String? = null,
        val fileName: String? = null,
        val contentType: String? = null
    )

    // ── Result type ───────────────────────────────────────────────────────────

    sealed class HarParseResult {
        data class Success(val log: HarLog) : HarParseResult()
        data class Failure(val message: String) : HarParseResult()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun parseFromFile(file: File): HarParseResult {
        return try {
            parseFromJson(file.readText())
        } catch (e: IOException) {
            HarParseResult.Failure("Failed to read file: ${e.message}")
        }
    }

    fun parseFromJson(json: String): HarParseResult {
        return try {
            val harFile = JsonService.mapper.readValue(json, HarFile::class.java)

            // Filter malformed entries defensively (e.g. missing required fields)
            val validEntries = harFile.log.entries.filter { entry ->
                entry.request.method.isNotBlank() && entry.request.url.isNotBlank()
            }
            val cleanLog = harFile.log.copy(entries = validEntries)

            HarParseResult.Success(cleanLog)
        } catch (e: JsonProcessingException) {
            HarParseResult.Failure("Invalid JSON format: ${e.message}")
        } catch (e: Exception) {
            HarParseResult.Failure("Failed to parse HAR: ${e.message}")
        }
    }
}

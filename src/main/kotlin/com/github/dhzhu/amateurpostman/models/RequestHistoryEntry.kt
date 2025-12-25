package com.github.dhzhu.amateurpostman.models

import java.time.Instant

/**
 * Represents a single entry in the request history
 */
data class RequestHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = Instant.now().toEpochMilli(),
    val request: HttpRequest,
    val response: HttpResponse? = null,
    val name: String? = null
) {
    /**
     * Get a display name for this history entry
     */
    fun getDisplayName(): String {
        return name ?: "${request.method} ${getShortUrl()}"
    }

    /**
     * Get a shortened URL for display
     */
    private fun getShortUrl(): String {
        val url = request.url
        return if (url.length > 50) {
            url.take(47) + "..."
        } else {
            url
        }
    }

    /**
     * Get formatted timestamp for display
     */
    fun getFormattedTime(): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }

    /**
     * Get status indicator string
     */
    fun getStatusIndicator(): String {
        return when {
            response == null -> "⏳"
            response.isSuccessful -> "✓ ${response.statusCode}"
            else -> "✗ ${response.statusCode}"
        }
    }
}

/**
 * State class for serialization with PersistentStateComponent
 */
data class RequestHistoryState(
    var entries: MutableList<SerializableHistoryEntry> = mutableListOf()
) {
    companion object {
        const val MAX_ENTRIES = 100
    }
}

/**
 * Serializable version of history entry for persistence
 */
data class SerializableHistoryEntry(
    var id: String = "",
    var timestamp: Long = 0,
    var url: String = "",
    var method: String = "GET",
    var headers: Map<String, String> = emptyMap(),
    var body: String? = null,
    var contentType: String? = null,
    var responseStatusCode: Int? = null,
    var responseStatusMessage: String? = null,
    var responseDuration: Long? = null,
    var name: String? = null
) {
    fun toHistoryEntry(): RequestHistoryEntry {
        val request = HttpRequest(
            url = url,
            method = HttpMethod.valueOf(method),
            headers = headers,
            body = body,
            contentType = contentType
        )
        
        val response = if (responseStatusCode != null) {
            HttpResponse(
                statusCode = responseStatusCode!!,
                statusMessage = responseStatusMessage ?: "",
                headers = emptyMap(),
                body = "",
                duration = responseDuration ?: 0,
                isSuccessful = responseStatusCode!! in 200..299
            )
        } else null
        
        return RequestHistoryEntry(
            id = id,
            timestamp = timestamp,
            request = request,
            response = response,
            name = name
        )
    }

    companion object {
        fun fromHistoryEntry(entry: RequestHistoryEntry): SerializableHistoryEntry {
            return SerializableHistoryEntry(
                id = entry.id,
                timestamp = entry.timestamp,
                url = entry.request.url,
                method = entry.request.method.name,
                headers = entry.request.headers,
                body = entry.request.body,
                contentType = entry.request.contentType,
                responseStatusCode = entry.response?.statusCode,
                responseStatusMessage = entry.response?.statusMessage,
                responseDuration = entry.response?.duration,
                name = entry.name
            )
        }
    }
}

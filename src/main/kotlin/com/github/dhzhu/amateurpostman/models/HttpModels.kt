package com.github.dhzhu.amateurpostman.models

/** Represents the type of HTTP request body content */
enum class BodyType(val displayName: String, val mimeType: String, val fileExtension: String) {
    TEXT("Text", "text/plain", "txt"),
    JSON("JSON", "application/json", "json"),
    XML("XML", "application/xml", "xml"),
    HTML("HTML", "text/html", "html"),
    JAVASCRIPT("JavaScript", "application/javascript", "js"),
    MULTIPART("Multipart", "multipart/form-data", "txt");

    companion object {
        fun fromMimeType(mimeType: String?): BodyType {
            return entries.find { it.mimeType.equals(mimeType, ignoreCase = true) } ?: JSON
        }
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
        val body: HttpBody? = null
) {
    /** @deprecated Use body.type.mimeType instead */
    val contentType: String? get() = body?.type?.mimeType
}

/** Represents an HTTP response received from the server */
data class HttpResponse(
        val statusCode: Int,
        val statusMessage: String,
        val headers: Map<String, List<String>>,
        val body: String,
        val duration: Long, // Duration in milliseconds
        val isSuccessful: Boolean = statusCode in 200..299
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

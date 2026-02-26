package com.github.dhzhu.amateurpostman.models

/** Represents the type of HTTP request body content */
enum class BodyType(val displayName: String, val mimeType: String, val fileExtension: String) {
    TEXT("Text", "text/plain", "txt"),
    JSON("JSON", "application/json", "json"),
    XML("XML", "application/xml", "xml"),
    HTML("HTML", "text/html", "html"),
    JAVASCRIPT("JavaScript", "application/javascript", "js"),
    MULTIPART("Multipart", "multipart/form-data", "txt"),
    GRAPHQL("GraphQL", "application/json", "graphql");

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

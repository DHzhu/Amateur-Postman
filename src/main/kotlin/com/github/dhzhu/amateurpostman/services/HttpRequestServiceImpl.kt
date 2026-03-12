package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.BodyType
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.models.MultipartPart
import com.github.dhzhu.amateurpostman.utils.VariableResolver
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Headers

/** Implementation of HttpRequestService using OkHttp */
@Service(Service.Level.PROJECT)
class HttpRequestServiceImpl(private val project: Project) : HttpRequestService, Disposable {

    private val logger = thisLogger()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }

    override fun dispose() {
        logger.info("Disposing HttpRequestServiceImpl, closing OkHttpClient resources")
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
    }

    override suspend fun executeRequest(request: HttpRequest): HttpResponse =
            withContext(Dispatchers.IO) {
                // Check for cancellation before starting
                coroutineContext.ensureActive()

                // Substitute variables from environment service
                val environmentService = project.service<EnvironmentService>()
                val variables = environmentService.getAllVariables()
                val processedRequest = if (variables.isNotEmpty()) {
                    logger.debug("Substituting ${variables.size} variables in request")
                    VariableResolver.substitute(request, variables)
                } else {
                    logger.debug("No variables defined, using request as-is")
                    request
                }

                logger.info("Executing ${processedRequest.method} request to ${processedRequest.url}")

                val startTime = System.currentTimeMillis()

                // Create event listener for profiling
                val eventListener = AmEventListener()

                // Create a client with the event listener for this request
                val clientWithListener = client.newBuilder()
                    .eventListener(eventListener)
                    .build()

                try {
                    val okHttpRequest = buildOkHttpRequest(processedRequest)
                    // Execute the call with profiling enabled
                    clientWithListener.newCall(okHttpRequest).execute().use { response ->
                        // Check for cancellation after receiving response
                        coroutineContext.ensureActive()

                        val duration = System.currentTimeMillis() - startTime
                        val responseBody = response.body?.string() ?: ""

                        val httpResponse =
                                HttpResponse(
                                        statusCode = response.code,
                                        statusMessage = response.message,
                                        headers = response.headers.toMultimap(),
                                        body = responseBody,
                                        duration = duration,
                                        isSuccessful = response.isSuccessful,
                                        profilingData = eventListener.getProfilingData()
                                )

                        logger.info(
                                "Request completed in ${duration}ms with status ${response.code}"
                        )

                        // Log profiling details if available
                        val profiling = eventListener.getProfilingData()
                        if (profiling != com.github.dhzhu.amateurpostman.models.HttpProfilingData.Empty) {
                            logger.debug(
                                "Profiling: DNS=${profiling.dnsDuration}ms, " +
                                "TCP=${profiling.tcpDuration}ms, " +
                                "SSL=${profiling.sslDuration}ms, " +
                                "TTFB=${profiling.ttfbDuration}ms, " +
                                "ConnectionReused=${profiling.connectionReused}"
                            )
                        }

                        httpResponse
                    }
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    // Log full stack trace internally for debugging
                    logger.warn("Request failed after ${duration}ms", e)

                    // Return user-friendly error response without exposing stack trace
                    val userMessage = when (e) {
                        is java.net.UnknownHostException -> "Unable to resolve host: ${e.message}"
                        is java.net.ConnectException -> "Connection refused: ${e.message}"
                        is java.net.SocketTimeoutException -> "Request timed out"
                        is javax.net.ssl.SSLException -> "SSL error: ${e.message}"
                        else -> "Request failed: ${e.message ?: "Unknown error"}"
                    }

                    HttpResponse(
                            statusCode = 0,
                            statusMessage = e.message ?: "Request failed",
                            headers = emptyMap(),
                            body = "Error: $userMessage",
                            duration = duration,
                            isSuccessful = false
                    )
                }
            }

    private fun buildOkHttpRequest(request: HttpRequest): Request {
        val builder = Request.Builder().url(request.url)

        // Add headers (merged with authentication headers if present)
        request.getEffectiveHeaders().forEach { (key, value) -> builder.addHeader(key, value) }

        // Set method and body
        when (request.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> {
                val body = createRequestBody(request)
                builder.post(body)
            }
            HttpMethod.PUT -> {
                val body = createRequestBody(request)
                builder.put(body)
            }
            HttpMethod.DELETE -> {
                val body = request.body?.let { createRequestBody(request) }
                if (body != null) {
                    builder.delete(body)
                } else {
                    builder.delete()
                }
            }
            HttpMethod.PATCH -> {
                val body = createRequestBody(request)
                builder.patch(body)
            }
            HttpMethod.HEAD -> builder.head()
            HttpMethod.OPTIONS -> builder.method("OPTIONS", null)
        }

        return builder.build()
    }

    private fun createRequestBody(request: HttpRequest): okhttp3.RequestBody {
        val httpBody = request.body

        // Handle Multipart body type
        if (httpBody?.type == BodyType.MULTIPART && httpBody.multipartData != null) {
            return createMultipartRequestBody(httpBody.multipartData)
        }

        // Handle standard body types
        val bodyContent = httpBody?.content ?: ""
        val mediaType =
                httpBody?.type?.mimeType?.toMediaTypeOrNull()
                        ?: "text/plain; charset=utf-8".toMediaTypeOrNull()
        return bodyContent.toRequestBody(mediaType)
    }

    private fun createMultipartRequestBody(parts: List<MultipartPart>): RequestBody {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        parts.forEach { part ->
            when (part) {
                is MultipartPart.TextField -> {
                    if (part.contentType != null) {
                        // Custom content-type: use addPart with explicit headers
                        // because addFormDataPart doesn't support custom content-type for text fields
                        val requestBody = part.value.toRequestBody(part.contentType.toMediaTypeOrNull())
                        builder.addPart(
                            Headers.headersOf("Content-Disposition", "form-data; name=\"${part.key}\""),
                            requestBody
                        )
                    } else {
                        // Default behavior: use addFormDataPart with default text/plain
                        builder.addFormDataPart(part.key, part.value)
                    }
                }
                is MultipartPart.FileField -> {
                    val file = File(part.filePath)
                    if (file.exists()) {
                        val mediaType = part.contentType?.toMediaTypeOrNull()
                            ?: file.extension.toContentTypeOrNull()
                        val requestBody = file.asRequestBody(mediaType)
                        builder.addFormDataPart(part.key, part.fileName, requestBody)
                    } else {
                        logger.warn("File not found: ${part.filePath}, adding as text field")
                        builder.addFormDataPart(part.key, "")
                    }
                }
            }
        }

        return builder.build()
    }

    private fun String.toContentTypeOrNull() = when (lowercase()) {
        "json" -> "application/json".toMediaTypeOrNull()
        "xml" -> "application/xml".toMediaTypeOrNull()
        "jpg", "jpeg" -> "image/jpeg".toMediaTypeOrNull()
        "png" -> "image/png".toMediaTypeOrNull()
        "gif" -> "image/gif".toMediaTypeOrNull()
        "pdf" -> "application/pdf".toMediaTypeOrNull()
        "txt" -> "text/plain".toMediaTypeOrNull()
        "html" -> "text/html".toMediaTypeOrNull()
        else -> "application/octet-stream".toMediaTypeOrNull()
    }
}

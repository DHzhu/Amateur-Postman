package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Implementation of HttpRequestService using OkHttp */
@Service(Service.Level.PROJECT)
class HttpRequestServiceImpl(private val project: Project) : HttpRequestService {

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

    override suspend fun executeRequest(request: HttpRequest): HttpResponse =
            withContext(Dispatchers.IO) {
                logger.info("Executing ${request.method} request to ${request.url}")

                val startTime = System.currentTimeMillis()

                try {
                    val okHttpRequest = buildOkHttpRequest(request)
                    client.newCall(okHttpRequest).execute().use { response ->
                        val duration = System.currentTimeMillis() - startTime
                        val responseBody = response.body?.string() ?: ""

                        val httpResponse =
                                HttpResponse(
                                        statusCode = response.code,
                                        statusMessage = response.message,
                                        headers = response.headers.toMultimap(),
                                        body = responseBody,
                                        duration = duration,
                                        isSuccessful = response.isSuccessful
                                )

                        logger.info("Request completed in ${duration}ms with status ${response.code}")
                        httpResponse
                    }
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    logger.warn("Request failed after ${duration}ms", e)

                    // Return error response
                    HttpResponse(
                            statusCode = 0,
                            statusMessage = e.message ?: "Request failed",
                            headers = emptyMap(),
                            body = "Error: ${e.message}\n\n${e.stackTraceToString()}",
                            duration = duration,
                            isSuccessful = false
                    )
                }
            }

    private fun buildOkHttpRequest(request: HttpRequest): Request {
        val builder = Request.Builder().url(request.url)

        // Add headers
        request.headers.forEach { (key, value) -> builder.addHeader(key, value) }

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
        val bodyContent = request.body ?: ""
        val mediaType =
                request.contentType?.toMediaTypeOrNull()
                        ?: "text/plain; charset=utf-8".toMediaTypeOrNull()
        return bodyContent.toRequestBody(mediaType)
    }
}

package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse

/** Service interface for executing HTTP requests */
interface HttpRequestService {
    /**
     * Execute an HTTP request asynchronously
     * @param request The HTTP request to execute
     * @return The HTTP response
     * @throws Exception if the request fails
     */
    suspend fun executeRequest(request: HttpRequest): HttpResponse
}

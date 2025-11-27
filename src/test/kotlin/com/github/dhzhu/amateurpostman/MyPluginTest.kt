package com.github.dhzhu.amateurpostman

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.services.HttpRequestService
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Tests for Amateur-Postman plugin */
class MyPluginTest : BasePlatformTestCase() {

    fun testHttpRequestService() {
        val httpService = project.service<HttpRequestService>()
        assertNotNull(httpService)
    }

    fun testHttpRequestCreation() {
        val request =
                HttpRequest(
                        url = "https://jsonplaceholder.typicode.com/posts/1",
                        method = HttpMethod.GET,
                        headers = emptyMap()
                )

        assertEquals("https://jsonplaceholder.typicode.com/posts/1", request.url)
        assertEquals(HttpMethod.GET, request.method)
    }
}

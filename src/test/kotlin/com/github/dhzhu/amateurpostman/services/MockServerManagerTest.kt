package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.concurrent.TimeUnit

class MockServerManagerTest {

    private lateinit var mockServerManager: MockServerManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        mockServerManager = MockServerManager()
    }

    @After
    fun tearDown() {
        mockServerManager.stop()
        mockServerManager.clearRules()
    }

    // ========== Server Lifecycle Tests ==========

    @Test
    fun `test server starts on auto-selected port`() = runBlocking {
        val port = mockServerManager.start(0)

        assertNotNull(port)
        assertTrue(port!! > 0)
        assertTrue(mockServerManager.isRunning)
        assertEquals(port, mockServerManager.runningPort)
    }

    @Test
    fun `test server starts on specified port`() = runBlocking {
        val specifiedPort = 18765
        val port = mockServerManager.start(specifiedPort)

        assertEquals(specifiedPort, port)
        assertEquals(specifiedPort, mockServerManager.runningPort)
    }

    @Test
    fun `test server stops correctly`() = runBlocking {
        mockServerManager.start(0)
        assertTrue(mockServerManager.isRunning)

        mockServerManager.stop()
        assertFalse(mockServerManager.isRunning)
        assertNull(mockServerManager.runningPort)
    }

    @Test
    fun `test starting already running server returns same port`() = runBlocking {
        val port1 = mockServerManager.start(0)
        val port2 = mockServerManager.start(0)

        assertEquals(port1, port2)
    }

    // ========== Rule Management Tests ==========

    @Test
    fun `test add rule`() {
        val rule = MockRule.create("/api/test", HttpMethod.GET, """{"status":"ok"}""")
        val added = mockServerManager.addRule(rule)

        assertEquals(rule.id, added.id)
        assertEquals(1, mockServerManager.allRules.size)
    }

    @Test
    fun `test remove rule`() {
        val rule = MockRule.create("/api/test", HttpMethod.GET)
        mockServerManager.addRule(rule)

        val removed = mockServerManager.removeRule(rule.id)

        assertTrue(removed)
        assertTrue(mockServerManager.allRules.isEmpty())
    }

    @Test
    fun `test remove non-existent rule returns false`() {
        val removed = mockServerManager.removeRule("non-existent-id")
        assertFalse(removed)
    }

    @Test
    fun `test get rule by id`() {
        val rule = MockRule.create("/api/test", HttpMethod.GET)
        mockServerManager.addRule(rule)

        val retrieved = mockServerManager.getRule(rule.id)

        assertNotNull(retrieved)
        assertEquals(rule.path, retrieved?.path)
    }

    @Test
    fun `test clear all rules`() {
        mockServerManager.addRule(MockRule.create("/api/test1", HttpMethod.GET))
        mockServerManager.addRule(MockRule.create("/api/test2", HttpMethod.POST))

        mockServerManager.clearRules()

        assertTrue(mockServerManager.allRules.isEmpty())
    }

    @Test
    fun `test find matching rule`() {
        val rule = MockRule.create("/api/users", HttpMethod.GET, """{"users":[]}""")
        mockServerManager.addRule(rule)

        val found = mockServerManager.findMatchingRule("/api/users", HttpMethod.GET)

        assertNotNull(found)
        assertEquals("/api/users", found?.path)
    }

    @Test
    fun `test find matching rule returns null for no match`() {
        val rule = MockRule.create("/api/users", HttpMethod.GET)
        mockServerManager.addRule(rule)

        val found = mockServerManager.findMatchingRule("/api/posts", HttpMethod.GET)

        assertNull(found)
    }

    @Test
    fun `test find matching rule respects method`() {
        val getRule = MockRule.create("/api/data", HttpMethod.GET, """{"method":"GET"}""")
        val postRule = MockRule.create("/api/data", HttpMethod.POST, """{"method":"POST"}""")
        mockServerManager.addRule(getRule)
        mockServerManager.addRule(postRule)

        val foundGet = mockServerManager.findMatchingRule("/api/data", HttpMethod.GET)
        val foundPost = mockServerManager.findMatchingRule("/api/data", HttpMethod.POST)

        assertEquals(HttpMethod.GET, foundGet?.method)
        assertEquals(HttpMethod.POST, foundPost?.method)
    }

    @Test
    fun `test disabled rule does not match`() {
        val rule = MockRule.create("/api/test", HttpMethod.GET).copy(enabled = false)
        mockServerManager.addRule(rule)

        val found = mockServerManager.findMatchingRule("/api/test", HttpMethod.GET)

        assertNull(found)
    }

    // ========== HTTP Request Tests ==========

    @Test
    fun `test GET request returns mock response`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/hello", HttpMethod.GET, """{"message":"Hello World"}""")
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/hello")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertTrue(response.body?.string()?.contains("Hello World") == true)
    }

    @Test
    fun `test POST request returns mock response`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/data", HttpMethod.POST, """{"created":true}""", 201)
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/data")
            .post("{}".toRequestBody())
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(201, response.code)
        assertTrue(response.body?.string()?.contains("created") == true)
    }

    @Test
    fun `test custom headers in response`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/custom",
                method = HttpMethod.GET,
                headers = mapOf("X-Custom-Header" to "CustomValue")
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/custom")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals("CustomValue", response.header("X-Custom-Header"))
    }

    @Test
    fun `test custom status code`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule.create("/api/notfound", HttpMethod.GET, """{"error":"Not found"}""", 404)
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/notfound")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(404, response.code)
    }

    @Test
    fun `test no matching rule returns 404`() = runBlocking {
        val port = mockServerManager.start(0)!!

        val request = Request.Builder()
            .url("http://localhost:$port/api/nonexistent")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(404, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("No mock rule found"))
    }

    @Test
    fun `test response delay`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/slow",
                method = HttpMethod.GET,
                body = """{"slow":true}""",
                delayMs = 100
            )
        )

        val startTime = System.currentTimeMillis()
        val request = Request.Builder()
            .url("http://localhost:$port/api/slow")
            .get()
            .build()
        httpClient.newCall(request).execute()
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("Response should be delayed by at least 100ms", elapsed >= 100)
    }

    @Test
    fun `test multiple rules with different paths`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(MockRule.create("/api/users", HttpMethod.GET, """{"users":[]}"""))
        mockServerManager.addRule(MockRule.create("/api/posts", HttpMethod.GET, """{"posts":[]}"""))
        mockServerManager.addRule(MockRule.create("/api/comments", HttpMethod.GET, """{"comments":[]}"""))

        val usersResponse = httpClient.newCall(
            Request.Builder().url("http://localhost:$port/api/users").get().build()
        ).execute()
        val postsResponse = httpClient.newCall(
            Request.Builder().url("http://localhost:$port/api/posts").get().build()
        ).execute()
        val commentsResponse = httpClient.newCall(
            Request.Builder().url("http://localhost:$port/api/comments").get().build()
        ).execute()

        assertTrue(usersResponse.body?.string()?.contains("users") == true)
        assertTrue(postsResponse.body?.string()?.contains("posts") == true)
        assertTrue(commentsResponse.body?.string()?.contains("comments") == true)
    }

    @Test
    fun `test PUT and DELETE methods`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(MockRule.create("/api/resource", HttpMethod.PUT, """{"updated":true}"""))
        mockServerManager.addRule(MockRule.create("/api/resource", HttpMethod.DELETE, """{"deleted":true}"""))

        val putResponse = httpClient.newCall(
            Request.Builder().url("http://localhost:$port/api/resource")
                .put("{}".toRequestBody()).build()
        ).execute()
        val deleteResponse = httpClient.newCall(
            Request.Builder().url("http://localhost:$port/api/resource")
                .delete().build()
        ).execute()

        assertTrue(putResponse.body?.string()?.contains("updated") == true)
        assertTrue(deleteResponse.body?.string()?.contains("deleted") == true)
    }
}
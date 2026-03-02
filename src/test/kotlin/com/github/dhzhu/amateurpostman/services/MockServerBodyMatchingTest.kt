package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.BodyMatcher
import com.github.dhzhu.amateurpostman.models.BodyMatchMode
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for Mock Server body matching functionality.
 */
class MockServerBodyMatchingTest {

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

    // ========== Body Matching Tests ==========

    @Test
    fun `test exact body match`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/data",
                method = HttpMethod.POST,
                body = """{"matched":"exact"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.EXACT, """{"name":"test"}""")
            )
        )

        // Send request with matching body
        val request = Request.Builder()
            .url("http://localhost:$port/api/data")
            .post("""{"name":"test"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertTrue(response.body?.string()?.contains("exact") == true)
    }

    @Test
    fun `test exact body no match`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/data",
                method = HttpMethod.POST,
                body = """{"matched":"exact"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.EXACT, """{"name":"test"}""")
            )
        )

        // Send request with non-matching body
        val request = Request.Builder()
            .url("http://localhost:$port/api/data")
            .post("""{"name":"different"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(404, response.code)
    }

    @Test
    fun `test contains body match`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/search",
                method = HttpMethod.POST,
                body = """{"result":"found"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.CONTAINS, "keyword")
            )
        )

        // Send request containing the keyword
        val request = Request.Builder()
            .url("http://localhost:$port/api/search")
            .post("""{"query":"search for keyword in data"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertTrue(response.body?.string()?.contains("found") == true)
    }

    @Test
    fun `test regex body match`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/users",
                method = HttpMethod.POST,
                body = """{"matched":"regex"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.REGEX, """"email"\s*:\s*"[^"]+@example\.com"""")
            )
        )

        // Send request matching the regex pattern
        val request = Request.Builder()
            .url("http://localhost:$port/api/users")
            .post("""{"name":"John","email":"john@example.com"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertTrue(response.body?.string()?.contains("regex") == true)
    }

    @Test
    fun `test regex body no match`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/users",
                method = HttpMethod.POST,
                body = """{"matched":"regex"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.REGEX, """"email"\s*:\s*"[^"]+@example\.com"""")
            )
        )

        // Send request NOT matching the regex pattern (different domain)
        val request = Request.Builder()
            .url("http://localhost:$port/api/users")
            .post("""{"name":"John","email":"john@other.com"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(404, response.code)
    }

    @Test
    fun `test none body match ignores body`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/any",
                method = HttpMethod.POST,
                body = """{"any":"body"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.NONE, "")
            )
        )

        // Send request with any body
        val request = Request.Builder()
            .url("http://localhost:$port/api/any")
            .post("""{"random":"content"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }

    @Test
    fun `test multiple rules with different body matchers`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Rule 1: exact match for user creation
        mockServerManager.addRule(
            MockRule(
                path = "/api/action",
                method = HttpMethod.POST,
                body = """{"action":"created"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.EXACT, """{"type":"create"}""")
            )
        )

        // Rule 2: regex match for update
        mockServerManager.addRule(
            MockRule(
                path = "/api/action",
                method = HttpMethod.POST,
                body = """{"action":"updated"}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.REGEX, """"type"\s*:\s*"update"""")
            )
        )

        // Test create action
        val createRequest = Request.Builder()
            .url("http://localhost:$port/api/action")
            .post("""{"type":"create"}""".toRequestBody("application/json".toMediaType()))
            .build()
        val createResponse = httpClient.newCall(createRequest).execute()
        assertTrue(createResponse.body?.string()?.contains("created") == true)

        // Test update action
        val updateRequest = Request.Builder()
            .url("http://localhost:$port/api/action")
            .post("""{"type":"update","id":123}""".toRequestBody("application/json".toMediaType()))
            .build()
        val updateResponse = httpClient.newCall(updateRequest).execute()
        assertTrue(updateResponse.body?.string()?.contains("updated") == true)
    }

    @Test
    fun `test body matcher with special characters`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/special",
                method = HttpMethod.POST,
                body = """{"special":true}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.CONTAINS, "special-key_123")
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/special")
            .post("""{"key":"special-key_123"}""".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }

    @Test
    fun `test body matcher with multiline regex`() = runBlocking {
        val port = mockServerManager.start(0)!!
        mockServerManager.addRule(
            MockRule(
                path = "/api/multiline",
                method = HttpMethod.POST,
                body = """{"multiline":true}""",
                bodyMatcher = BodyMatcher(BodyMatchMode.REGEX, """(?s)line1.*line2""")
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/multiline")
            .post("line1\nsome content\nline2".toRequestBody("text/plain".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
    }
}
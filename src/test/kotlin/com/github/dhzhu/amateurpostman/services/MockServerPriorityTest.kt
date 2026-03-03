package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for Mock Server rule priority handling.
 * These tests verify that when multiple rules match, the highest priority rule is selected.
 */
class MockServerPriorityTest {

    private lateinit var mockServerManager: MockServerManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    @BeforeEach
    fun setUp() {
        mockServerManager = MockServerManager()
    }

    @AfterEach
    fun tearDown() {
        mockServerManager.stop()
        mockServerManager.clearRules()
    }

    // ========== Priority Tests ==========

    /**
     * Test: When multiple rules match the same path and method,
     * the rule with higher priority should be selected.
     */
    @Test
    fun `test higher priority rule wins over lower priority`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Add lower priority rule first (simulating default response)
        mockServerManager.addRule(
            MockRule(
                path = "/api/users",
                method = HttpMethod.GET,
                body = """{"priority":"low","message":"default response"}""",
                priority = 10
            )
        )

        // Add higher priority rule (simulating override)
        mockServerManager.addRule(
            MockRule(
                path = "/api/users",
                method = HttpMethod.GET,
                body = """{"priority":"high","message":"override response"}""",
                priority = 100
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/users")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("high"), "Expected high priority response, got: $body")
        assertTrue(body.contains("override response"), "Expected override response, got: $body")
    }

    /**
     * Test: Rules without explicit priority should use default priority.
     * Default priority should be lower than explicitly set high priority.
     */
    @Test
    fun `test default priority is lower than explicit high priority`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Add rule without priority (should use default, e.g., 0 or 50)
        mockServerManager.addRule(
            MockRule(
                path = "/api/data",
                method = HttpMethod.GET,
                body = """{"priority":"default"}"""
                // No priority specified, should use default
            )
        )

        // Add rule with high priority
        mockServerManager.addRule(
            MockRule(
                path = "/api/data",
                method = HttpMethod.GET,
                body = """{"priority":"high"}""",
                priority = 100
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/data")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("high"), "Expected high priority response, got: $body")
    }

    /**
     * Test: When multiple rules have the same priority, the behavior should be deterministic.
     * The rule added first (or last) should consistently win.
     */
    @Test
    fun `test same priority rules have deterministic behavior`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Add two rules with the same priority
        mockServerManager.addRule(
            MockRule(
                path = "/api/conflict",
                method = HttpMethod.GET,
                body = """{"rule":"first"}""",
                priority = 50
            )
        )

        mockServerManager.addRule(
            MockRule(
                path = "/api/conflict",
                method = HttpMethod.GET,
                body = """{"rule":"second"}""",
                priority = 50
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/conflict")
            .get()
            .build()

        // Multiple requests should return consistent results
        val responses = (1..5).map {
            httpClient.newCall(request).execute().body?.string()
        }

        // All responses should be the same (deterministic)
        assertTrue(responses.toSet().size == 1, "Responses should be deterministic, got: $responses")
    }

    /**
     * Test: Priority should work with body matching rules.
     */
    @Test
    fun `test priority with body matching rules`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Lower priority catch-all rule
        mockServerManager.addRule(
            MockRule(
                path = "/api/action",
                method = HttpMethod.POST,
                body = """{"matched":"catch-all"}""",
                priority = 10
            )
        )

        // Higher priority specific rule
        mockServerManager.addRule(
            MockRule(
                path = "/api/action",
                method = HttpMethod.POST,
                body = """{"matched":"specific"}""",
                priority = 100
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/action")
            .post(okhttp3.RequestBody.create(null, byteArrayOf()))
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("specific"), "Expected specific (high priority) response, got: $body")
    }

    /**
     * Test: Negative priority values should work correctly.
     */
    @Test
    fun `test negative priority values`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Rule with negative priority
        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"negative"}""",
                priority = -100
            )
        )

        // Rule with zero priority (default)
        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"zero"}""",
                priority = 0
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/test")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        // Zero priority should win over negative
        assertTrue(body.contains("zero"), "Expected zero priority response, got: $body")
    }

    /**
     * Test: Very high priority values should work correctly.
     */
    @Test
    fun `test very high priority values`() = runBlocking {
        val port = mockServerManager.start(0)!!

        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"normal"}""",
                priority = 100
            )
        )

        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"very-high"}""",
                priority = Int.MAX_VALUE
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/test")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("very-high"), "Expected very-high priority response, got: $body")
    }

    /**
     * Test: findMatchingRule should return highest priority rule.
     */
    @Test
    fun `test findMatchingRule returns highest priority`() {
        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"low"}""",
                priority = 10
            )
        )

        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"high"}""",
                priority = 100
            )
        )

        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"medium"}""",
                priority = 50
            )
        )

        val matchedRule = mockServerManager.findMatchingRule("/api/test", HttpMethod.GET)

        assertNotNull(matchedRule)
        assertEquals(100, matchedRule?.priority, "Expected highest priority rule to be matched")
        assertTrue(matchedRule?.body?.contains("high") == true, "Expected high priority rule body")
    }

    /**
     * Test: Disabled rules should be ignored even if they have higher priority.
     */
    @Test
    fun `test disabled rules are ignored regardless_of_priority`() = runBlocking {
        val port = mockServerManager.start(0)!!

        // Add enabled rule with low priority
        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"low-enabled"}""",
                priority = 10,
                enabled = true
            )
        )

        // Add disabled rule with high priority
        mockServerManager.addRule(
            MockRule(
                path = "/api/test",
                method = HttpMethod.GET,
                body = """{"priority":"high-disabled"}""",
                priority = 100,
                enabled = false
            )
        )

        val request = Request.Builder()
            .url("http://localhost:$port/api/test")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body?.string() ?: ""
        // Low priority enabled rule should win over high priority disabled rule
        assertTrue(body.contains("low-enabled"), "Expected enabled low priority response, got: $body")
    }
}
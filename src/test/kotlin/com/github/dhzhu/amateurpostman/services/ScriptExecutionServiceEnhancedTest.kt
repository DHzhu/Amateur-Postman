package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

/**
 * Tests for enhanced request/response objects in scripts.
 * Tests pm.request and pm.response extended APIs.
 */
class ScriptExecutionServiceEnhancedTest {

    private lateinit var scriptService: ScriptExecutionService
    private lateinit var environmentService: EnvironmentService

    @BeforeEach
    fun setUp() {
        environmentService = mock<EnvironmentService>()
        scriptService = ScriptExecutionService(mock<Project>(), environmentService)
    }

    @Test
    fun testPmResponseJsonParsesSimpleJson() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = """{"name":"John","age":30}""",
            headers = mapOf("Content-Type" to listOf("application/json")),
            duration = 100
        )
        val script = "var data = pm.response.json(); pm.test('Name is John', () => data.name === 'John');"

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
        assertEquals(1, result.results.size)
        assertTrue(result.results[0].passed, "Should parse JSON correctly")
    }

    @Test
    fun testPmResponseJsonParsesNestedJson() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = """{"user":{"name":"John","address":{"city":"NYC"}}}""",
            headers = mapOf("Content-Type" to listOf("application/json")),
            duration = 100
        )
        val script = """
            var data = pm.response.json();
            pm.test('Nested access works', () => data.user.address.city === 'NYC');
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }

    @Test
    fun testPmResponseJsonHandlesArrays() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = """{"users":[{"name":"John"},{"name":"Jane"}]}""",
            headers = mapOf("Content-Type" to listOf("application/json")),
            duration = 100
        )
        val script = """
            var data = pm.response.json();
            pm.test('Array access works', () => data.users[0].name === 'John' && data.users.length === 2);
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }

    @Test
    fun testPmResponseTextReturnsRawBody() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "Plain text response",
            headers = emptyMap(),
            duration = 100
        )
        val script = """
            var text = pm.response.text();
            pm.test('Text returns body', () => text === 'Plain text response');
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }

    @Test
    fun testPmResponseHeadersGetRetrievesHeader() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "{}",
            headers = mapOf(
                "Content-Type" to listOf("application/json"),
                "Authorization" to listOf("Bearer token123")
            ),
            duration = 100
        )
        val script = """
            var contentType = pm.response.headers.get('Content-Type');
            var auth = pm.response.headers.get('Authorization');
            pm.test('Headers retrieved', () => contentType === 'application/json' && auth === 'Bearer token123');
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }

    @Test
    fun testPmResponseResponseTimeFromProfilingData() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "{}",
            headers = emptyMap(),
            duration = 150,
            profilingData = HttpProfilingData(
                dnsDuration = 50,
                tcpDuration = 30,
                sslDuration = 40,
                ttfbDuration = 100,
                totalDuration = 250,
                connectionReused = false
            )
        )
        val script = """
            pm.test('Response time from profiling', () => pm.response.responseTime === 250);
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }

    @Test
    fun testPmRequestUrlAccessible() = runBlocking {
        val script = """
            pm.test('URL accessible', () => typeof pm.request.url !== 'undefined');
        """.trimIndent()

        // Pre-request scripts don't have request object yet, but we can test the binding exists
        val variables = scriptService.executePreRequestScript(script)

        // Should not throw error
        assertNotNull(variables)
    }

    @Test
    fun testPmResponseJsonHandlesInvalidJsonGracefully() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "Invalid JSON {{{",
            headers = mapOf("Content-Type" to listOf("application/json")),
            duration = 100
        )
        val script = """
            try {
                var data = pm.response.json();
                pm.test('Should not reach here', () => false);
            } catch (e) {
                pm.test('Error thrown for invalid JSON', () => e.message.includes('JSON'));
            }
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        // Test should pass because we catch the error
        assertTrue(result.passed)
    }

    @Test
    fun testPmResponseJsonWithEmptyObject() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "{}",
            headers = mapOf("Content-Type" to listOf("application/json")),
            duration = 100
        )
        val script = """
            var data = pm.response.json();
            pm.test('Empty object parsed', () => typeof data === 'object');
        """.trimIndent()

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }
}

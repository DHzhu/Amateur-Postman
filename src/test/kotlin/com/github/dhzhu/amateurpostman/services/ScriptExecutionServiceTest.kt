package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.intellij.openapi.project.Project
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class ScriptExecutionServiceTest {

    private lateinit var scriptService: ScriptExecutionService
    private lateinit var environmentService: EnvironmentService

    @BeforeEach
    fun setUp() {
        environmentService = mock<EnvironmentService>()
        scriptService = ScriptExecutionService(mock<Project>(), environmentService)
    }

    @Test
    fun `test pre-request script can set environment variable`() = runBlocking {
        val script = "am.environment.set('test_key', 'test_value')"

        scriptService.executePreRequestScript(script)

        verify(environmentService).setVariableInCurrent("test_key", "test_value")
    }

    @Test
    fun `test test script can assert status code`() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "OK",
            headers = emptyMap(),
            duration = 100
        )
        val script = "pm.expect.statusCode(200)"

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
        assertEquals(1, result.results.size)
        assertTrue(result.results[0].passed)
    }

    @Test
    fun `test test script fails on wrong status code`() = runBlocking {
        val response = HttpResponse(
            statusCode = 404,
            statusMessage = "Not Found",
            body = "Not Found",
            headers = emptyMap(),
            duration = 100
        )
        val script = "pm.expect.statusCode(200)"

        val result = scriptService.executeTestScript(script, response)

        assertFalse(result.passed)
        assertFalse(result.results[0].passed)
    }

    @Test
    fun `test test script can assert body content`() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "{\"status\": \"success\"}",
            headers = emptyMap(),
            duration = 100
        )
        val script = "pm.expect.body.toContain('success')"

        val result = scriptService.executeTestScript(script, response)

        assertTrue(result.passed)
    }

    @Test
    fun `test script execution error handling`() = runBlocking {
        val response = HttpResponse(
            statusCode = 200,
            statusMessage = "OK",
            body = "OK",
            headers = emptyMap(),
            duration = 100
        )
        val script = "pm.invalid.call()"

        val result = scriptService.executeTestScript(script, response)

        assertFalse(result.passed)
        assertTrue(result.results[0].message.contains("Error"))
    }

    /**
     * Stress test for concurrent script execution.
     * Verifies that the Mutex properly serializes engine calls
     * and prevents context contamination between concurrent executions.
     */
    @Test
    fun `test concurrent pre-request scripts do not contaminate each other`() = runBlocking {
        val numConcurrent = 20
        val results = (1..numConcurrent).map { i ->
            async {
                val script = "am.environment.set('thread_id', '$i'); am.environment.get('thread_id')"
                scriptService.executePreRequestScript(script) to i
            }
        }.awaitAll()

        // Each script should have its own isolated context
        // With Mutex protection, no contamination should occur
        results.forEach { (variables, expectedId) ->
            assertEquals(expectedId.toString(), variables["thread_id"], "Context contamination detected")
        }
    }

    /**
     * Stress test for concurrent test script execution.
     * Verifies that concurrent test scripts execute correctly without interference.
     */
    @Test
    fun `test concurrent test scripts execute correctly`() = runBlocking {
        val numConcurrent = 20
        val results = (1..numConcurrent).map { i ->
            async {
                val response = HttpResponse(
                    statusCode = i * 10, // Unique status code for each
                    statusMessage = "OK",
                    body = "Response $i",
                    headers = emptyMap(),
                    duration = 100
                )
                val script = "pm.expect.statusCode(${i * 10})"
                scriptService.executeTestScript(script, response) to i
            }
        }.awaitAll()

        // All tests should pass without interference
        results.forEach { (result, _) ->
            assertTrue(result.passed, "Concurrent test script failed unexpectedly")
            assertEquals(1, result.results.size)
        }
    }

    /**
     * Mixed concurrent pre-request and test script execution stress test.
     * Verifies that both script types can execute concurrently without interference.
     */
    @Test
    fun `test mixed concurrent scripts execute correctly`() = runBlocking {
        val numConcurrent = 10
        val preRequestJobs = (1..numConcurrent).map { i ->
            async {
                val script = "am.environment.set('pre_id', '$i')"
                scriptService.executePreRequestScript(script)
            }
        }
        val testJobs = (1..numConcurrent).map { i ->
            async {
                val response = HttpResponse(
                    statusCode = 200,
                    statusMessage = "OK",
                    body = "Test $i",
                    headers = emptyMap(),
                    duration = 100
                )
                val script = "pm.expect.body.toContain('$i')"
                scriptService.executeTestScript(script, response)
            }
        }

        val preResults = preRequestJobs.awaitAll()
        val testResults = testJobs.awaitAll()

        // All pre-request scripts should complete without error
        preResults.forEach { variables ->
            assertTrue(variables.containsKey("pre_id"), "Pre-request script should complete")
        }

        // All test scripts should pass
        testResults.forEach { result ->
            assertTrue(result.passed, "Test script should pass")
        }
    }
}

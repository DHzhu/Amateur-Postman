package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ScriptExecutionServiceTest {

    private lateinit var scriptService: ScriptExecutionService
    private lateinit var environmentService: EnvironmentService

    @Before
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
}

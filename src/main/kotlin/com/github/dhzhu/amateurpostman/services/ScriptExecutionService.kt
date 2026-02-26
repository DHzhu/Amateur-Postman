package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of executing a test script.
 *
 * @property passed Whether all tests passed
 * @property results List of individual test results
 */
data class TestResult(
    val passed: Boolean,
    val results: List<AssertionResult>
) {
    companion object {
        fun create(results: List<AssertionResult>): TestResult {
            return TestResult(
                passed = results.all { it.passed },
                results = results
            )
        }
    }

    /**
     * Returns a summary string for display.
     */
    fun getSummary(): String {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        return "Passed: $passed | Failed: $failed"
    }
}

/**
 * Result of a single assertion.
 *
 * @property name The test/assertion name
 * @property passed Whether the assertion passed
 * @property message Optional message (error message if failed, or success message)
 */
data class AssertionResult(
    val name: String,
    val passed: Boolean,
    val message: String = ""
)

/**
 * Context object available in Pre-request scripts.
 *
 * Provides access to environment variables and utility functions.
 */
class PreRequestContext(
    private val environmentService: EnvironmentService,
    private val temporaryVariables: MutableMap<String, String> = mutableMapOf()
) {
    /**
     * Sets an environment variable.
     */
    fun set(key: String, value: String) {
        temporaryVariables[key] = value
        // Also update the current environment
        environmentService.setVariableInCurrent(key, value)
    }

    /**
     * Gets an environment variable.
     */
    fun get(key: String): String? {
        return temporaryVariables[key] ?: environmentService.getVariableFromCurrent(key)
    }

    /**
     * Gets all variables (temporary + environment).
     */
    fun getVariables(): Map<String, String> {
        val envVars = environmentService.getCurrentEnvironmentVariables()
        return temporaryVariables + envVars
    }

    /**
     * Returns the current timestamp in milliseconds.
     */
    fun timestamp(): Long = System.currentTimeMillis()

    /**
     * Returns a random UUID.
     */
    fun uuid(): String = java.util.UUID.randomUUID().toString()

    /**
     * Returns a random integer between min and max (inclusive).
     */
    fun randomInt(min: Int = 0, max: Int = Int.MAX_VALUE - 1): Int {
        return kotlin.random.Random.nextInt(min, max + 1)
    }
}

/**
 * Context object available in Test scripts.
 *
 * Provides access to response data and assertion functions.
 */
class TestContext(private val response: HttpResponse) {
    private val assertions = mutableListOf<AssertionResult>()

    /**
     * Adds a test assertion.
     *
     * @param name The test name
     * @param fn The test function - should return true if test passes
     */
    fun test(name: String, fn: () -> Boolean) {
        val passed = try {
            fn()
        } catch (e: Exception) {
            false
        }
        assertions.add(AssertionResult(name, passed))
    }

    /**
     * Asserts that the response has a specific status code.
     */
    fun assertStatusCode(expectedCode: Int) {
        test("Status code is $expectedCode") {
            response.statusCode == expectedCode
        }
    }

    /**
     * Asserts that the response body contains a specific string.
     */
    fun assertBodyContains(text: String) {
        test("Response body contains \"$text\"") {
            response.body.contains(text)
        }
    }

    /**
     * Asserts that a header exists and has a specific value.
     */
    fun assertHeader(name: String, value: String) {
        test("Header \"$name\" is \"$value\"") {
            val headerValues = response.headers[name]
            headerValues?.any { it.equals(value, ignoreCase = true) } == true
        }
    }

    /**
     * Asserts that the response time is less than a threshold.
     */
    fun assertResponseTimeLessThan(maxMs: Long) {
        test("Response time is less than ${maxMs}ms") {
            response.duration < maxMs
        }
    }

    /**
     * Gets all assertion results.
     */
    fun getResults(): List<AssertionResult> = assertions.toList()
}

/**
 * Service for executing Pre-request and Test scripts.
 *
 * Uses JavaScript (Nashorn) for script execution with sandboxed contexts.
 */
@Service(Service.Level.PROJECT)
class ScriptExecutionService(private val project: Project) {

    private val engine = javax.script.ScriptEngineManager().getEngineByName("nashorn")!!

    /**
     * Executes a Pre-request script.
     *
     * @param script The JavaScript code to execute
     * @return Map of temporary variables set during script execution
     */
    suspend fun executePreRequestScript(script: String): Map<String, String> = withContext(Dispatchers.IO) {
        if (script.isBlank()) {
            return@withContext emptyMap()
        }

        try {
            val environmentService = project.service<EnvironmentService>()
            val context = PreRequestContext(environmentService)
            val bindings = engine.createBindings()

            // Expose context functions to script
            val envMap = mutableMapOf<String, Any>()
            val setFunc = { key: String, value: String -> context.set(key, value) }
            val getFunc = { key: String -> context.get(key) }
            envMap["set"] = setFunc
            envMap["get"] = getFunc

            val amMap = mutableMapOf<String, Any>()
            amMap["environment"] = envMap
            amMap["timestamp"] = { context.timestamp() }
            amMap["uuid"] = { context.uuid() }
            amMap["randomInt"] = { min: Int, max: Int -> context.randomInt(min, max) }

            bindings["am"] = amMap

            engine.eval(script, bindings)
            context.getVariables()
        } catch (e: Exception) {
            // Log error but don't fail the request
            thisLogger().warn("Pre-request script execution failed: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Executes a Test script against a response.
     *
     * @param script The JavaScript code to execute
     * @param response The HTTP response to test
     * @return TestResult with assertion results
     */
    suspend fun executeTestScript(
        script: String,
        response: HttpResponse
    ): TestResult = withContext(Dispatchers.IO) {
        if (script.isBlank()) {
            return@withContext TestResult.create(emptyList())
        }

        try {
            val context = TestContext(response)
            val bindings = engine.createBindings()

            // Expose context functions to script
            val pmMap = mutableMapOf<String, Any>()
            pmMap["test"] = { name: String, fn: () -> Unit -> context.test(name) { fn(); true } }

            val expectMap = mutableMapOf<String, Any>()
            expectMap["statusCode"] = { code: Int -> context.assertStatusCode(code) }
            val bodyMap = mutableMapOf<String, Any>()
            bodyMap["toContain"] = { text: String -> context.assertBodyContains(text) }
            expectMap["body"] = bodyMap
            pmMap["expect"] = expectMap

            val respMap = mutableMapOf<String, Any>()
            respMap["code"] = response.statusCode
            respMap["body"] = response.body
            respMap["headers"] = response.headers
            respMap["responseTime"] = response.duration
            pmMap["response"] = respMap

            bindings["pm"] = pmMap

            engine.eval(script, bindings)
            TestResult.create(context.getResults())
        } catch (e: Exception) {
            // Log error and return failed result
            thisLogger().warn("Test script execution failed: ${e.message}")
            TestResult.create(listOf(
                AssertionResult("Script execution", false, "Error: ${e.message}")
            ))
        }
    }

    companion object {
        fun getInstance(project: Project): ScriptExecutionService {
            return project.service()
        }
    }
}

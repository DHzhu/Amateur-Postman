package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.Variable
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.util.Base64
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess

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
    private val collectionId: String? = null,
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
     * Sets a global variable.
     */
    fun setGlobal(key: String, value: String) {
        environmentService.setGlobalVariable(Variable(key, value))
    }

    /**
     * Gets a global variable.
     */
    fun getGlobal(key: String): String? {
        val normalizedKey = Variable.normalizeKey(key)
        return environmentService.getGlobalVariablesMap()[normalizedKey]
    }

    /**
     * Sets a collection variable.
     */
    fun setCollectionVariable(key: String, value: String) {
        collectionId?.let { id ->
            environmentService.setCollectionVariable(id, Variable(key, value))
        }
    }

    /**
     * Gets a collection variable.
     */
    fun getCollectionVariable(key: String): String? {
        return collectionId?.let { id ->
            environmentService.getCollectionVariables(id).getVariableValue(key)
        }
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
class TestContext(
    private val response: HttpResponse,
    private val request: HttpRequest? = null
) {
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

// ── Binding classes exposed to JS scripts ────────────────────────────────────
// GraalVM JS resolves `obj.method(args)` via invokeMember on the Java object.
// Using real Kotlin classes with named methods (instead of Map<String, Any>)
// ensures GraalVM can find and call them correctly.

class AmEnvironmentBinding(private val context: PreRequestContext) {
    fun set(key: String, value: String) = context.set(key, value)
    fun get(key: String): String? = context.get(key)
}

class AmGlobalsBinding(private val context: PreRequestContext) {
    fun set(key: String, value: String) = context.setGlobal(key, value)
    fun get(key: String): String? = context.getGlobal(key)
}

class AmCollectionVariablesBinding(private val context: PreRequestContext) {
    fun set(key: String, value: String) = context.setCollectionVariable(key, value)
    fun get(key: String): String? = context.getCollectionVariable(key)
}

class AmBinding(private val context: PreRequestContext) {
    // @JvmField makes this a public Java field so GraalVM JS can read it via readMember().
    // Without it, Kotlin compiles val to a private field + public getter, which GraalVM
    // in polyglot mode cannot map to JS property access automatically.
    @JvmField val environment = AmEnvironmentBinding(context)
    @JvmField val globals = AmGlobalsBinding(context)
    @JvmField val collectionVariables = AmCollectionVariablesBinding(context)
    fun timestamp(): Long = context.timestamp()
    fun uuid(): String = context.uuid()
    fun randomInt(min: Int, max: Int): Int = context.randomInt(min, max)
}

class PmExpectBodyBinding(private val context: TestContext) {
    fun toContain(text: String) = context.assertBodyContains(text)
}

class PmExpectBinding(private val context: TestContext) {
    @JvmField val body = PmExpectBodyBinding(context)
    fun statusCode(code: Int) = context.assertStatusCode(code)
    fun header(name: String, value: String) = context.assertHeader(name, value)
}

class PmResponseHeadersBinding(private val headers: Map<String, List<String>>) {
    private val lowerCaseHeaders = headers.mapKeys { it.key.lowercase() }

    fun get(name: String): String? {
        return lowerCaseHeaders[name.lowercase()]?.firstOrNull()
    }

    fun getAll(name: String): List<String>? {
        return lowerCaseHeaders[name.lowercase()]
    }
}

class PmResponseBinding(private val response: HttpResponse) {
    @JvmField val code: Int = response.statusCode
    @JvmField val body: String = response.body
    @JvmField val headers: PmResponseHeadersBinding = PmResponseHeadersBinding(response.headers)

    // Use profiling total time if available, otherwise use duration
    @JvmField val responseTime: Long = response.profilingData?.totalDuration ?: response.duration

    private val gson = Gson()

    /**
     * Parse response body as JSON.
     * @return Parsed JSON object (can be Object, Array, or primitive)
     * @throws Exception if JSON is invalid
     */
    fun json(): Any {
        val parsed = JsonParser.parseString(response.body)
        return when {
            parsed.isJsonObject -> gson.fromJson(parsed, Map::class.java)
            parsed.isJsonArray -> gson.fromJson(parsed, List::class.java)
            parsed.isJsonPrimitive -> {
                val prim = parsed.asJsonPrimitive
                when {
                    prim.isBoolean -> prim.asBoolean
                    prim.isNumber -> prim.asNumber
                    else -> prim.asString
                }
            }
            else -> response.body
        }
    }

    /**
     * Get response body as raw text.
     */
    fun text(): String = response.body
}

class PmRequestBinding(private val request: HttpRequest) {
    @JvmField val url: String = request.url
    @JvmField val method: String = request.method.name
    @JvmField val headers: Map<String, String> = request.headers

    fun getHeader(name: String): String? {
        val lowerKey = name.lowercase()
        for ((key, value) in request.headers) {
            if (key.lowercase() == lowerKey) {
                return value
            }
        }
        return null
    }

    fun getBody(): String? = request.body?.content
}

class PmBinding(
    private val httpResponse: HttpResponse,
    private val context: TestContext,
    request: HttpRequest? = null
) {
    @JvmField val expect = PmExpectBinding(context)
    @JvmField val response = PmResponseBinding(httpResponse)
    @JvmField val request: PmRequestBinding? = request?.let { PmRequestBinding(it) }

    /**
     * Postman-style test function.
     * @param name Test name
     * @param fn Test function that returns true if test passes
     */
    fun test(name: String, fn: () -> Boolean) {
        context.test(name, fn)
    }
}
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Service for executing Pre-request and Test scripts.
 *
 * Uses GraalVM JS for script execution with sandboxed contexts.
 */
@Service(Service.Level.PROJECT)
class ScriptExecutionService(
    private val project: Project,
    private val environmentService: EnvironmentService
) {
    // Called by IntelliJ service framework (single-arg Project constructor)
    constructor(project: Project) : this(project, project.service())

    private val engine = try {
        // GraalVM 24.x defaults to a restrictive host access policy that prevents
        // JS from calling Java/Kotlin host objects. We must explicitly enable
        // HostAccess.ALL so that scripts can invoke Kotlin lambdas exposed via bindings.
        val jsEngine = GraalJSScriptEngine.create(
            null,
            Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup { true }
        )

        // Load crypto-js library
        loadCryptoJsLibrary(jsEngine)

        // Register global utility functions
        registerUtilityFunctions(jsEngine)

        jsEngine
    } catch (e: Exception) {
        thisLogger().error("Failed to initialize GraalVM JS engine", e)
        null
    }

    /**
     * Mutex to serialize GraalVM JS engine.eval() calls.
     * GraalJSScriptEngine is not thread-safe for concurrent operations,
     * even with separate bindings. This ensures no context contamination
     * between concurrent pre-request and test script executions.
     */
    private val scriptExecutionMutex = Mutex()

    /**
     * Loads crypto-js library into the JS engine.
     */
    private fun loadCryptoJsLibrary(engine: GraalJSScriptEngine) {
        try {
            val cryptoJsResource = this.javaClass.classLoader.getResourceAsStream("js/crypto-js.min.js")
            if (cryptoJsResource != null) {
                val cryptoJsCode = cryptoJsResource.bufferedReader().use { it.readText() }
                engine.eval(cryptoJsCode)
                thisLogger().info("crypto-js library loaded successfully")
            } else {
                thisLogger().warn("crypto-js library not found in resources")
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to load crypto-js library", e)
        }
    }

    /**
     * Registers global utility functions (atob, btoa).
     */
    private fun registerUtilityFunctions(engine: GraalJSScriptEngine) {
        try {
            // Register atob function using Java interop
            val bindings = engine.createBindings()
            bindings["atob"] = { str: String ->
                try {
                    String(Base64.getDecoder().decode(str))
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid Base64 input", e)
                }
            }
            bindings["btoa"] = { str: String ->
                Base64.getEncoder().encodeToString(str.toByteArray())
            }
            engine.eval("globalThis.atob = atob; globalThis.btoa = btoa;", bindings)

            thisLogger().info("Utility functions (atob, btoa) registered")
        } catch (e: Exception) {
            thisLogger().error("Failed to register utility functions", e)
        }
    }

    /**
     * Executes a Pre-request script.
     *
     * @param script The JavaScript code to execute
     * @param collectionId Optional collection ID for collection variable scope
     * @return Map of temporary variables set during script execution
     */
    suspend fun executePreRequestScript(
        script: String,
        collectionId: String? = null
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (script.isBlank() || engine == null) {
            if (engine == null && script.isNotBlank()) {
                thisLogger().warn("Skipping pre-request script: JS engine not available")
            }
            return@withContext emptyMap()
        }

        try {
            val context = PreRequestContext(environmentService, collectionId)
            val bindings = engine.createBindings()

            bindings["am"] = AmBinding(context)

            // Serialize script execution to prevent concurrent context contamination
            scriptExecutionMutex.withLock {
                engine.eval(script, bindings)
            }
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
        if (script.isBlank() || engine == null) {
            if (engine == null && script.isNotBlank()) {
                thisLogger().warn("Skipping test script: JS engine not available")
                return@withContext TestResult.create(listOf(
                    AssertionResult("Script execution", false, "Error: JS engine not available")
                ))
            }
            return@withContext TestResult.create(emptyList())
        }

        try {
            val context = TestContext(response)
            val bindings = engine.createBindings()

            bindings["pm"] = PmBinding(response, context)

            // Serialize script execution to prevent concurrent context contamination
            scriptExecutionMutex.withLock {
                engine.eval(script, bindings)
            }
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

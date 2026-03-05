package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.BodyType
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.models.Variable
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess

/**
 * Result of executing a test script.
 *
 * @property passed Whether all tests passed
 * @property results List of individual test results
 */
data class TestResult(val passed: Boolean, val results: List<AssertionResult>) {
    companion object {
        fun create(results: List<AssertionResult>): TestResult {
            return TestResult(passed = results.all { it.passed }, results = results)
        }
    }

    /** Returns a summary string for display. */
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
data class AssertionResult(val name: String, val passed: Boolean, val message: String = "")

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
    /** Sets an environment variable. */
    fun set(key: String, value: String) {
        temporaryVariables[key] = value
        // Also update the current environment
        environmentService.setVariableInCurrent(key, value)
    }

    /** Gets an environment variable. */
    fun get(key: String): String? {
        return temporaryVariables[key] ?: environmentService.getVariableFromCurrent(key)
    }

    /** Gets all variables (temporary + environment). */
    fun getVariables(): Map<String, String> {
        val envVars = environmentService.getCurrentEnvironmentVariables()
        return temporaryVariables + envVars
    }

    /** Sets a global variable. */
    fun setGlobal(key: String, value: String) {
        environmentService.setGlobalVariable(Variable(key, value))
    }

    /** Gets a global variable. */
    fun getGlobal(key: String): String? {
        val normalizedKey = Variable.normalizeKey(key)
        return environmentService.getGlobalVariablesMap()[normalizedKey]
    }

    /** Sets a collection variable. */
    fun setCollectionVariable(key: String, value: String) {
        collectionId?.let { id ->
            environmentService.setCollectionVariable(id, Variable(key, value))
        }
    }

    /** Gets a collection variable. */
    fun getCollectionVariable(key: String): String? {
        return collectionId?.let { id ->
            environmentService.getCollectionVariables(id).getVariableValue(key)
        }
    }

    /** Returns the current timestamp in milliseconds. */
    fun timestamp(): Long = System.currentTimeMillis()

    /** Returns a random UUID. */
    fun uuid(): String = java.util.UUID.randomUUID().toString()

    /** Returns a random integer between min and max (inclusive). */
    fun randomInt(min: Int = 0, max: Int = Int.MAX_VALUE - 1): Int {
        return kotlin.random.Random.nextInt(min, max + 1)
    }
}

/**
 * Context object available in Test scripts.
 *
 * Provides access to response data and assertion functions.
 */
class TestContext(private val response: HttpResponse, private val request: HttpRequest? = null) {
    private val assertions = mutableListOf<AssertionResult>()

    /**
     * Adds a test assertion from a Kotlin context (e.g. [TestContext.assertStatusCode]).
     *
     * @param name The test name
     * @param fn Kotlin lambda that returns true if the test passes
     */
    fun test(name: String, fn: () -> Boolean) {
        val passed =
                try {
                    fn()
                } catch (e: Exception) {
                    false
                }
        assertions.add(AssertionResult(name, passed))
    }

    /**
     * Records a pre-evaluated test result from the JS preamble layer.
     *
     * The JS preamble wrapper executes the user-supplied JS function itself and passes only the
     * primitive [Boolean] result here. This avoids passing JS function objects across the GraalVM
     * host/guest boundary, which triggers "unexpected interop primitive" errors.
     *
     * @param name Test name
     * @param passed Whether the test passed
     * @param error Error message if failed, empty string otherwise
     */
    fun recordTest(name: String, passed: Boolean, error: String) {
        assertions.add(AssertionResult(name, passed, error))
    }

    /** Asserts that the response has a specific status code. */
    fun assertStatusCode(expectedCode: Int) {
        test("Status code is $expectedCode") { response.statusCode == expectedCode }
    }

    /** Asserts that the response body contains a specific string. */
    fun assertBodyContains(text: String) {
        test("Response body contains \"$text\"") { response.body.contains(text) }
    }

    /** Asserts that a header exists and has a specific value. */
    fun assertHeader(name: String, value: String) {
        test("Header \"$name\" is \"$value\"") {
            val headerValues = response.headers[name]
            headerValues?.any { it.equals(value, ignoreCase = true) } == true
        }
    }

    /** Asserts that the response time is less than a threshold. */
    fun assertResponseTimeLessThan(maxMs: Long) {
        test("Response time is less than ${maxMs}ms") { response.duration < maxMs }
    }

    /** Gets all assertion results. */
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

    /** Get response body as raw text. */
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
        request: HttpRequest? = null,
        private val httpRequestService: HttpRequestService? = null
) {
    // Initially set to the basic expect binding, but can be overridden by chai.expect
    @JvmField var expect: Any = PmExpectBinding(context)
    @JvmField val response = PmResponseBinding(httpResponse)
    @JvmField val request: PmRequestBinding? = request?.let { PmRequestBinding(it) }

    /**
     * Postman-style test function for direct Kotlin callers. JS scripts use [recordTest] instead
     * (via the preamble wrapper).
     * @param name Test name
     * @param fn Kotlin lambda that returns true if test passes
     */
    fun test(name: String, fn: () -> Boolean) {
        context.test(name, fn)
    }

    /**
     * Records a pre-evaluated test result, called by the JS preamble wrapper. Accepts only plain
     * Java types to avoid GraalVM interop primitive errors.
     */
    fun recordTest(name: String, passed: Boolean, error: String) {
        context.recordTest(name, passed, error)
    }

    /**
     * Executes an HTTP request synchronously from within a JS script. Called by the JS-side
     * pm.sendRequest wrapper in the preamble.
     *
     * @param requestJson JSON string describing the request: {"url":"...", "method":"GET",
     * "header":{...}, "body":{"raw":"..."}} or just a URL string (handled by the JS wrapper before
     * this call)
     * @return JSON string: {"code":200, "body":"...", "headers":{...}} or {"_error":"message"} on
     * failure
     */
    fun sendRequest(requestJson: String): String {
        val svc =
                httpRequestService
                        ?: return Gson().toJson(
                                        mapOf("_error" to "pm.sendRequest is not available")
                                )
        val gson = Gson()
        return try {
            val req = parseSendRequest(requestJson)
            val resp = runBlocking { svc.executeRequest(req) }
            gson.toJson(
                    mapOf(
                            "code" to resp.statusCode,
                            "body" to resp.body,
                            "headers" to resp.headers.mapValues { it.value.firstOrNull() ?: "" }
                    )
            )
        } catch (e: Exception) {
            gson.toJson(mapOf("_error" to (e.message ?: "sendRequest failed")))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSendRequest(requestJson: String): HttpRequest {
        val map = Gson().fromJson(requestJson, Map::class.java) as Map<String, Any?>
        val url =
                map["url"] as? String
                        ?: throw IllegalArgumentException("pm.sendRequest: url is required")
        val method =
                try {
                    HttpMethod.valueOf((map["method"] as? String)?.uppercase() ?: "GET")
                } catch (e: IllegalArgumentException) {
                    HttpMethod.GET
                }
        val headers =
                buildMap<String, String> {
                    (map["header"] as? Map<*, *>)?.forEach { (k, v) ->
                        if (k is String && v is String) put(k, v)
                    }
                }
        val bodyMap = map["body"] as? Map<*, *>
        val body =
                bodyMap?.let {
                    val raw = it["raw"] as? String
                    if (!raw.isNullOrBlank()) HttpBody(raw, BodyType.JSON) else null
                }
        return HttpRequest(url = url, method = method, headers = headers, body = body)
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
        private val environmentService: EnvironmentService,
        private val httpRequestService: HttpRequestService? = null
) {
    // Called by IntelliJ service framework (single-arg Project constructor)
    constructor(project: Project) : this(project, project.service(), null)

    // Two-arg constructor retained for test compatibility
    constructor(
            project: Project,
            environmentService: EnvironmentService
    ) : this(project, environmentService, null)

    private fun getHttpService(): HttpRequestService? =
            httpRequestService
                    ?: try {
                        project.service<HttpRequestServiceImpl>()
                    } catch (e: Exception) {
                        null
                    }

    private val engine =
            try {
                // GraalVM 24.x defaults to a restrictive host access policy that prevents
                // JS from calling Java/Kotlin host objects. We must explicitly enable
                // HostAccess.ALL so that scripts can invoke Kotlin lambdas exposed via bindings.
                val jsEngine =
                        GraalJSScriptEngine.create(
                                null,
                                Context.newBuilder("js")
                                        .allowHostAccess(HostAccess.ALL)
                                        .allowHostClassLookup { true }
                        )

                // Load crypto-js library
                loadCryptoJsLibrary(jsEngine)

                // Load chai.js assertion library
                loadChaiLibrary(jsEngine)

                // Load ajv JSON Schema validation library
                loadAjvLibrary(jsEngine)

                // Register global utility functions
                registerUtilityFunctions(jsEngine)

                jsEngine
            } catch (e: Exception) {
                thisLogger().error("Failed to initialize GraalVM JS engine", e)
                null
            }

    /**
     * Mutex to serialize GraalVM JS engine.eval() calls. GraalJSScriptEngine is not thread-safe for
     * concurrent operations, even with separate bindings. This ensures no context contamination
     * between concurrent pre-request and test script executions.
     */
    private val scriptExecutionMutex = Mutex()

    /**
     * Whether chai.js was successfully loaded into the engine's global JS scope. Used to decide
     * whether to inject `var expect = chai.expect` in the preamble. We avoid storing the GraalVM JS
     * object reference itself, as injecting it across ScriptContexts (via Bindings) triggers
     * OtherContextGuestObject interop errors.
     */
    @Volatile private var hasChaiLoaded: Boolean = false

    /**
     * Whether ajv was successfully loaded into the engine's global JS scope. Same rationale as
     * [hasChaiLoaded]: accessed by name from JS, not via Java reference.
     */
    @Volatile private var hasAjvLoaded: Boolean = false

    /** Loads crypto-js library into the JS engine. */
    private fun loadCryptoJsLibrary(engine: GraalJSScriptEngine) {
        try {
            val cryptoJsResource =
                    this.javaClass.classLoader.getResourceAsStream("js/crypto-js.min.js")
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
     * Loads chai.js assertion library into the JS engine. Exposes chai.expect as global expect
     * function.
     */
    private fun loadChaiLibrary(engine: GraalJSScriptEngine) {
        try {
            // Define 'global' for chai.js compatibility with GraalVM JS
            engine.eval("if (typeof global === 'undefined') { var global = globalThis; }")

            val chaiResource = this.javaClass.classLoader.getResourceAsStream("js/chai.min.js")
            if (chaiResource != null) {
                val chaiCode = chaiResource.bufferedReader().use { it.readText() }
                engine.eval(chaiCode)
                // Disable Proxy mode globally and permanently for the lifetime of this engine.
                // chai v4+ defaults useProxy=true, which wraps assertion chains in ES6 Proxy
                // objects. GraalVM JS fallback runtime (no JVMCI/JIT) has a compatibility issue
                // with Reflect.get() on defineProperty getters inside Proxy traps, causing
                // "unexpected interop primitive" AssertionErrors when chai assertions run.
                // Setting useProxy=false once here makes chai fall back to plain
                // Object.defineProperty getters, which GraalVM handles correctly.
                engine.eval("chai.config.useProxy = false;")
                // Track availability by boolean flag, NOT by Java reference.
                // Storing the GraalVM JS Value and injecting it across ScriptContexts via
                // Bindings triggers OtherContextGuestObject.migrateReturn errors. The chai
                // object is accessible by name from the global JS scope instead.
                hasChaiLoaded = true
                thisLogger().info("chai.js library loaded successfully")
            } else {
                thisLogger().warn("chai.js library not found in resources")
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to load chai.js library", e)
        }
    }

    /**
     * Loads ajv v6 JSON Schema validation library into the JS engine. Exposes the Ajv constructor
     * globally for use in test scripts.
     */
    private fun loadAjvLibrary(engine: GraalJSScriptEngine) {
        try {
            val ajvResource = this.javaClass.classLoader.getResourceAsStream("js/ajv.min.js")
            if (ajvResource != null) {
                val ajvCode = ajvResource.bufferedReader().use { it.readText() }
                engine.eval(ajvCode)
                // Check whether Ajv was actually registered in global scope.
                // Use local val — we no longer keep a field reference to the GraalVM JS object
                // to avoid cross-ScriptContext interop errors.
                val ajvPresent = engine.eval("typeof Ajv !== 'undefined'")
                if (ajvPresent == true || ajvPresent?.toString() == "true") {
                    hasAjvLoaded = true
                    thisLogger().info("ajv library loaded successfully")
                } else {
                    thisLogger()
                            .warn(
                                    "ajv library loaded but Ajv constructor not found in global scope"
                            )
                }
            } else {
                thisLogger().warn("ajv library not found in resources")
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to load ajv library", e)
        }
    }

    /** Registers global utility functions (atob, btoa). */
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
     * Builds a JavaScript preamble that wraps the Java pm host object in a native JS object and
     * optionally sets up the global expect from chai.
     *
     * Root cause: pm.response.json() returns Gson-parsed Java Map/List objects. GraalVM exposes
     * these as "foreign" objects in JS, causing chai's strict type assertions (equal,
     * be.a('number'), Array.isArray) to fail because Java String/Number/List are not the same as
     * native JS primitives.
     *
     * Fix: override pm.response.json() to return JSON.parse(body), which produces genuine native JS
     * objects that chai can assert against correctly.
     */
    private fun buildPmWrapperScript(withChai: Boolean): String = buildString {
        appendLine(
                """
            pm = (function(__jpm) {
                // Force JS string coercion so body.includes() and other JS string
                // methods work correctly regardless of GraalVM's Java-String wrapping.
                var __body = String(__jpm.response.body);

                var __resp = {
                    code: __jpm.response.code,
                    body: __body,
                    responseTime: __jpm.response.responseTime,
                    headers: {
                        get: function(h) { return __jpm.response.headers.get(h); },
                        getAll: function(h) { return __jpm.response.headers.getAll(h); }
                    },
                    json: function() { return JSON.parse(__body); },
                    text: function() { return __body; }
                };

                // pm.response.to assertion chain (Postman-compatible shorthand)
                // Usage: pm.response.to.have.status(200);  pm.response.to.be.ok;
                // Throws on failure — wrap in pm.test() for proper test recording.
                (function() {
                    var __tobe = {};
                    ['ok', 'success'].forEach(function(p) {
                        Object.defineProperty(__tobe, p, { get: function() {
                            if (__resp.code < 200 || __resp.code >= 300)
                                throw new Error('Expected 2xx status but got ' + __resp.code);
                            return true;
                        }});
                    });
                    Object.defineProperty(__tobe, 'notFound', { get: function() {
                        if (__resp.code !== 404) throw new Error('Expected 404 but got ' + __resp.code);
                        return true;
                    }});
                    Object.defineProperty(__tobe, 'error', { get: function() {
                        if (__resp.code < 400) throw new Error('Expected 4xx/5xx but got ' + __resp.code);
                        return true;
                    }});
                    Object.defineProperty(__tobe, 'json', { get: function() {
                        var ct = String(__resp.headers.get('content-type') || '');
                        if (!ct.toLowerCase().includes('json'))
                            throw new Error('Expected JSON content-type but got "' + ct + '"');
                        return true;
                    }});
                    __resp.to = {
                        have: {
                            status: function(code) {
                                if (__resp.code !== code)
                                    throw new Error('Expected status ' + code + ' but got ' + __resp.code);
                                return true;
                            },
                            header: function(name, value) {
                                var raw = __resp.headers.get(name);
                                if (raw === null || raw === undefined)
                                    throw new Error('Expected header "' + name + '" to exist');
                                if (value !== undefined && String(raw) !== String(value))
                                    throw new Error('Expected header "' + name + '" to be "' + value + '" but got "' + String(raw) + '"');
                                return true;
                            },
                            body: function(text) {
                                if (!__body.includes(String(text)))
                                    throw new Error('Expected body to include "' + text + '"');
                                return true;
                            },
                            jsonSchema: function(schema) {
                                if (typeof Ajv === 'undefined')
                                    throw new Error('ajv is not loaded — cannot validate JSON Schema');
                                var ajv = new Ajv({allErrors: true});
                                var valid = ajv.validate(schema, __resp.json());
                                if (!valid)
                                    throw new Error('Schema validation failed: ' + ajv.errorsText());
                                return true;
                            }
                        },
                        be: __tobe
                    };
                })();

                return {
                    // Execute JS test function in JS, pass only primitive result to Java.
                    // Passing JS function objects to Java triggers GraalVM "unexpected interop
                    // primitive" AssertionErrors, so we resolve the Boolean here.
                    test: function(n, f) {
                        var __passed = false;
                        var __err = '';
                        try {
                            var __r = f();
                            __passed = (__r === undefined || __r === null || __r !== false);
                        } catch(e) {
                            __passed = false;
                            __err = e ? String(e.message || e) : 'unknown error';
                        }
                        __jpm.recordTest(n, __passed, __err);
                    },
                    expect: __jpm.expect,
                    response: __resp,
                    request: __jpm.request ? {
                        url: __jpm.request.url,
                        method: __jpm.request.method,
                        headers: __jpm.request.headers,
                        getHeader: function(h) { return __jpm.request.getHeader(h); },
                        getBody: function() { return __jpm.request.getBody(); }
                    } : null,
                    sendRequest: function(req, callback) {
                        var reqJson = typeof req === 'string'
                            ? JSON.stringify({url: req, method: 'GET'})
                            : JSON.stringify(req);
                        var resultJson = __jpm.sendRequest(reqJson);
                        var result = JSON.parse(resultJson);
                        if (!callback) return;
                        if (result._error) {
                            callback(result._error, null);
                        } else {
                            var resp = {
                                code: result.code,
                                body: result.body,
                                headers: {
                                    get: function(h) {
                                        var lh = h.toLowerCase();
                                        for (var k in result.headers) {
                                            if (k.toLowerCase() === lh) return result.headers[k];
                                        }
                                        return null;
                                    }
                                },
                                json: function() { return JSON.parse(result.body); },
                                text: function() { return result.body; }
                            };
                            callback(null, resp);
                        }
                    }
                };
            })(pm);
        """.trimIndent()
        )
        if (withChai) {
            // Disable chai's Proxy wrapper — GraalVM JS fallback runtime has compatibility
            // issues with Reflect.get() on defineProperty getters inside Proxy traps.
            // Turning off useProxy makes chai fall back to plain Object.defineProperty getters,
            // which GraalVM handles correctly.
            appendLine("chai.config.useProxy = false;")
            appendLine("var expect = chai.expect;")
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
    ): Map<String, String> =
            withContext(Dispatchers.IO) {
                if (script.isBlank() || engine == null) {
                    if (engine == null && script.isNotBlank()) {
                        thisLogger().warn("Skipping pre-request script: JS engine not available")
                    }
                    return@withContext emptyMap()
                }

                try {
                    val context = PreRequestContext(environmentService, collectionId)

                    // Inject am into ENGINE_SCOPE (same pattern as executeTestScript) to avoid
                    // cross-ScriptContext OtherContextGuestObject interop errors.
                    scriptExecutionMutex.withLock {
                        val globalBindings =
                                engine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE)
                        globalBindings["am"] = AmBinding(context)
                        try {
                            engine.eval(script)
                        } finally {
                            globalBindings.remove("am")
                        }
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
    suspend fun executeTestScript(script: String, response: HttpResponse): TestResult =
            withContext(Dispatchers.IO) {
                if (script.isBlank() || engine == null) {
                    if (engine == null && script.isNotBlank()) {
                        thisLogger().warn("Skipping test script: JS engine not available")
                        return@withContext TestResult.create(
                                listOf(
                                        AssertionResult(
                                                "Script execution",
                                                false,
                                                "Error: JS engine not available"
                                        )
                                )
                        )
                    }
                    return@withContext TestResult.create(emptyList())
                }

                try {
                    val context = TestContext(response)

                    // Serialize script execution to prevent concurrent context contamination.
                    // Inject pm into the ENGINE_SCOPE (global ScriptContext) rather than into an
                    // isolated Bindings object. This is safe because scriptExecutionMutex ensures
                    // sequential access, and it allows the script to access global JS objects
                    // (chai, Ajv) without cross-ScriptContext object migration — which would
                    // trigger OtherContextGuestObject.migrateReturn "unexpected interop primitive"
                    // errors caused by passing GraalVM JS Values across ScriptContext boundaries.
                    scriptExecutionMutex.withLock {
                        val globalBindings =
                                engine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE)
                        globalBindings["pm"] =
                                PmBinding(response, context, httpRequestService = getHttpService())
                        try {
                            val fullScript = buildPmWrapperScript(hasChaiLoaded) + "\n" + script
                            @Suppress("kotlin:S2755") engine.eval(fullScript)
                        } finally {
                            // Clean up pm reference after execution to avoid stale state
                            globalBindings.remove("pm")
                        }
                    }
                    TestResult.create(context.getResults())
                } catch (e: Exception) {
                    // Log error and return failed result
                    thisLogger().warn("Test script execution failed: ${e.message}")
                    TestResult.create(
                            listOf(
                                    AssertionResult(
                                            "Script execution",
                                            false,
                                            "Error: ${e.message}"
                                    )
                            )
                    )
                }
            }

    companion object {
        fun getInstance(project: Project): ScriptExecutionService {
            return project.service()
        }
    }
}

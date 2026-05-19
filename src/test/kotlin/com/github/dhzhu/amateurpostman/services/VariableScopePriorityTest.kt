package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.CollectionVariables
import com.github.dhzhu.amateurpostman.models.Environment
import com.github.dhzhu.amateurpostman.models.Variable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for variable scope priority resolution.
 * Priority chain: Global (lowest) -> Collection -> Environment -> Temporary (highest).
 *
 * Tests the model-level logic directly without requiring IntelliJ service initialization.
 */
class VariableScopePriorityTest {

    // ========== Variable Model Tests ==========

    @Test
    fun testVariableKeyNormalization() {
        assertEquals("api_key", Variable.normalizeKey("API_KEY"))
        assertEquals("api_key", Variable.normalizeKey("api_key"))
        assertEquals("api_key", Variable.normalizeKey("Api_Key"))
    }

    @Test
    fun testDisabledVariableExcludedFromMap() {
        val env = Environment(
            id = "test", name = "Test",
            variables = listOf(
                Variable(key = "enabled_var", value = "yes", enabled = true),
                Variable(key = "disabled_var", value = "no", enabled = false)
            )
        )
        val map = env.getVariablesMap()

        assertEquals("yes", map["enabled_var"])
        assertNull(map["disabled_var"])
    }

    @Test
    fun testVariableNormalizedKeyMethod() {
        val variable = Variable(key = "BaseUrl", value = "https://api.example.com")
        assertEquals("baseurl", variable.normalizedKey())
    }

    // ========== Environment Variable Tests ==========

    @Test
    fun testEnvironmentGetVariablesMapCaseInsensitive() {
        val env = Environment(
            id = "test", name = "Test",
            variables = listOf(
                Variable(key = "API_KEY", value = "key123"),
                Variable(key = "Base_URL", value = "https://example.com")
            )
        )
        val map = env.getVariablesMap()

        assertEquals("key123", map["api_key"])
        assertEquals("https://example.com", map["base_url"])
    }

    @Test
    fun testEnvironmentSetVariableReplaces() {
        var env = Environment(
            id = "test", name = "Test",
            variables = listOf(Variable(key = "key", value = "old"))
        )
        env = env.setVariable(Variable(key = "key", value = "new"))

        assertEquals("new", env.getVariableValue("key"))
        assertEquals(1, env.variables.size)
    }

    @Test
    fun testEnvironmentRemoveVariable() {
        var env = Environment(
            id = "test", name = "Test",
            variables = listOf(
                Variable(key = "key1", value = "val1"),
                Variable(key = "key2", value = "val2")
            )
        )
        env = env.removeVariable("key1")

        assertNull(env.getVariableValue("key1"))
        assertEquals("val2", env.getVariableValue("key2"))
    }

    // ========== CollectionVariables Tests ==========

    @Test
    fun testCollectionVariablesGetMap() {
        val collVars = CollectionVariables(
            id = "cv-1", collectionId = "coll-1",
            variables = listOf(
                Variable(key = "COLL_KEY", value = "coll_value"),
                Variable(key = "SHARED", value = "from_collection")
            )
        )
        val map = collVars.getVariablesMap()

        assertEquals("coll_value", map["coll_key"])
        assertEquals("from_collection", map["shared"])
    }

    // ========== Priority Chain Tests ==========

    @Test
    fun testEnvironmentOverridesGlobal() {
        val global = Environment(
            id = "global", name = "Globals", isGlobal = true,
            variables = listOf(Variable(key = "url", value = "https://global.example.com"))
        )
        val environment = Environment(
            id = "env", name = "Dev",
            variables = listOf(Variable(key = "url", value = "https://dev.example.com"))
        )

        // Simulate getAllVariables priority: global then environment (environment wins)
        val merged = global.getVariablesMap().toMutableMap()
        merged.putAll(environment.getVariablesMap())

        assertEquals("https://dev.example.com", merged["url"])
    }

    @Test
    fun testCollectionOverridesGlobal() {
        val global = Environment(
            id = "global", name = "Globals", isGlobal = true,
            variables = listOf(Variable(key = "token", value = "global_token"))
        )
        val collVars = CollectionVariables(
            id = "cv", collectionId = "coll",
            variables = listOf(Variable(key = "token", value = "collection_token"))
        )

        // Simulate priority: global then collection (collection wins)
        val merged = global.getVariablesMap().toMutableMap()
        merged.putAll(collVars.getVariablesMap())

        assertEquals("collection_token", merged["token"])
    }

    @Test
    fun testEnvironmentOverridesCollection() {
        val collVars = CollectionVariables(
            id = "cv", collectionId = "coll",
            variables = listOf(Variable(key = "host", value = "coll.example.com"))
        )
        val environment = Environment(
            id = "env", name = "Prod",
            variables = listOf(Variable(key = "host", value = "prod.example.com"))
        )

        // Simulate priority: collection then environment (environment wins)
        val merged = collVars.getVariablesMap().toMutableMap()
        merged.putAll(environment.getVariablesMap())

        assertEquals("prod.example.com", merged["host"])
    }

    @Test
    fun testFullPriorityChainEnvironmentWins() {
        val global = Environment(
            id = "global", name = "Globals", isGlobal = true,
            variables = listOf(
                Variable(key = "api_url", value = "https://global.api.com"),
                Variable(key = "api_key", value = "global_key"),
                Variable(key = "shared", value = "global_shared")
            )
        )
        val collVars = CollectionVariables(
            id = "cv", collectionId = "coll",
            variables = listOf(
                Variable(key = "api_url", value = "https://coll.api.com"),
                Variable(key = "shared", value = "coll_shared")
            )
        )
        val environment = Environment(
            id = "env", name = "Dev",
            variables = listOf(
                Variable(key = "api_url", value = "https://dev.api.com")
            )
        )

        // Simulate EnvironmentService.getAllVariables: global -> collection -> environment
        val merged = global.getVariablesMap().toMutableMap()
        merged.putAll(collVars.getVariablesMap())
        merged.putAll(environment.getVariablesMap())

        // api_url: environment overrides all
        assertEquals("https://dev.api.com", merged["api_url"])
        // shared: collection overrides global
        assertEquals("coll_shared", merged["shared"])
        // api_key: only in global, preserved
        assertEquals("global_key", merged["api_key"])
    }

    @Test
    fun testDisabledVariablesNotInPriorityChain() {
        val global = Environment(
            id = "global", name = "Globals", isGlobal = true,
            variables = listOf(Variable(key = "key", value = "global", enabled = true))
        )
        val environment = Environment(
            id = "env", name = "Dev",
            variables = listOf(Variable(key = "key", value = "disabled_env", enabled = false))
        )

        val merged = global.getVariablesMap().toMutableMap()
        merged.putAll(environment.getVariablesMap())

        // Disabled environment variable does NOT override global
        assertEquals("global", merged["key"])
    }

    @Test
    fun testVariablesToMapExcludesDisabled() {
        val variables = listOf(
            Variable(key = "a", value = "1", enabled = true),
            Variable(key = "b", value = "2", enabled = false),
            Variable(key = "c", value = "3", enabled = true)
        )

        val map = variables.filter { it.enabled }.associate { it.normalizedKey() to it.value }

        assertEquals(2, map.size)
        assertEquals("1", map["a"])
        assertNull(map["b"])
        assertEquals("3", map["c"])
    }

    @Test
    fun testEmptyScopesProduceEmptyMaps() {
        val emptyEnv = Environment.create("Empty")
        val emptyCollVars = CollectionVariables.create("coll-id")

        assertTrue(emptyEnv.getVariablesMap().isEmpty())
        assertTrue(emptyCollVars.getVariablesMap().isEmpty())
    }
}

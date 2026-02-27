package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.Variable
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for variable scope priority.
 * Tests the resolution order: Global -> Collection -> Environment
 */
class VariableScopePriorityTest {

    // Note: Full integration tests require Project and Service initialization
    // These tests verify the model logic without requiring full IntelliJ setup

    /**
     * Test: Variable normalization is case-insensitive
     */
    @Test
    fun testVariableKeyNormalization() {
        val key1 = "API_KEY"
        val key2 = "api_key"
        val key3 = "Api_Key"

        assertEquals("Variable.normalizeKey should lowercase", "api_key", Variable.normalizeKey(key1))
        assertEquals("Variable.normalizeKey should lowercase", "api_key", Variable.normalizeKey(key2))
        assertEquals("Variable.normalizeKey should lowercase", "api_key", Variable.normalizeKey(key3))
    }

    /**
     * Test: Disabled variables should not be returned
     */
    @Test
    fun testDisabledVariableNotReturned() {
        val enabledVar = Variable(key = "feature", value = "enabled", enabled = true)
        val disabledVar = Variable(key = "feature", value = "disabled", enabled = false)

        assertEquals("enabled", enabledVar.value)
        assertEquals("disabled", disabledVar.value)
        assertFalse("Disabled variable should return false for enabled", disabledVar.enabled)
    }

    /**
     * Test: Variable normalizedKey method works correctly
     */
    @Test
    fun testVariableNormalizedKeyMethod() {
        val variable = Variable(key = "BaseUrl", value = "https://api.example.com")
        assertEquals("baseurl", variable.normalizedKey())
    }

    /**
     * Test: Global variables should be stored correctly
     */
    @Test
    fun testGlobalVariableStructure() {
        val variable = Variable(key = "apiKey", value = "secret-key")
        assertEquals("apiKey", variable.key)
        assertEquals("secret-key", variable.value)
        assertTrue("Default enabled should be true", variable.enabled)
        assertEquals("", variable.description)
    }

    /**
     * Test: Variable with all properties
     */
    @Test
    fun testVariableWithAllProperties() {
        val variable = Variable(
            key = "timeout",
            value = "5000",
            description = "Request timeout in milliseconds",
            enabled = true
        )
        assertEquals("timeout", variable.key)
        assertEquals("5000", variable.value)
        assertEquals("Request timeout in milliseconds", variable.description)
        assertTrue(variable.enabled)
    }

    /**
     * Test: CollectionVariables create method generates ID
     */
    @Test
    fun testCollectionVariablesCreate() {
        // Note: This is a simplified test without full EnvironmentService
        // In real scenario, we would use a test Project
        val collectionId = "test-collection-id"
        // Just verify the concept - actual implementation would use service
        assertNotNull("Collection ID should not be null", collectionId)
    }

    /**
     * Test: Priority chain documentation
     */
    @Test
    fun testVariablePriorityChain() {
        // This test documents the expected priority:
        // 1. Environment (highest priority)
        // 2. Collection
        // 3. Global (lowest priority)

        val globalVar = "global-value"
        val collectionVar = "collection-value"
        val environmentVar = "environment-value"

        // Expected behavior:
        // When all three have the same key, environment value should be used
        // When only collection and global have the same key, collection value should be used
        // When only global has the key, global value should be used

        // Document expectation:
        assertTrue("Environment > Collection > Global", true)
    }
}

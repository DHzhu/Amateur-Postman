package com.github.dhzhu.amateurpostman.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EnvironmentModelsTest {

    private lateinit var testEnvironment: Environment

    @Before
    fun setup() {
        testEnvironment = Environment(
            id = "test-env-1",
            name = "Test Environment",
            variables = listOf(
                Variable("baseUrl", "https://api.test.com", "Base URL for API"),
                Variable("apiKey", "test-key-123", "API Key"),
                Variable("timeout", "30000", "Request timeout", enabled = false),
                Variable("BaseUrl", "https://override.test.com", "Override with different case")
            )
        )
    }

    @Test
    fun `Variable should normalize keys correctly`() {
        val variable = Variable("MyVariable", "value")
        assertEquals("myvariable", variable.normalizedKey())
        assertEquals("myvariable", Variable.normalizeKey("MyVariable"))
        assertEquals("myvariable", Variable.normalizeKey("MYVARIABLE"))
        assertEquals("myvariable", Variable.normalizeKey("myVariable"))
    }

    @Test
    fun `Environment should get variable value case-insensitively`() {
        // Should find first match ( baseUrl )
        assertEquals("https://api.test.com", testEnvironment.getVariableValue("baseUrl"))
        assertEquals("https://api.test.com", testEnvironment.getVariableValue("BASEURL"))
        assertEquals("https://api.test.com", testEnvironment.getVariableValue("BaseUrl"))
    }

    @Test
    fun `Environment should return null for non-existent variable`() {
        assertNull(testEnvironment.getVariableValue("nonExistent"))
        assertNull(testEnvironment.getVariableValue("missing"))
    }

    @Test
    fun `Environment should return null for disabled variable`() {
        assertNull(testEnvironment.getVariableValue("timeout"))
        assertNull(testEnvironment.getVariableValue("TIMEOUT"))
    }

    @Test
    fun `Environment should check if variable exists`() {
        assertTrue(testEnvironment.hasVariable("baseUrl"))
        assertTrue(testEnvironment.hasVariable("BASEURL"))
        assertTrue(testEnvironment.hasVariable("apiKey"))
        assertFalse(testEnvironment.hasVariable("nonExistent"))
    }

    @Test
    fun `Environment should set new variable`() {
        val result = testEnvironment.setVariable(
            Variable("newVar", "newValue", "New variable")
        )

        assertEquals(5, result.variables.size)
        assertEquals("newValue", result.getVariableValue("newVar"))
        assertEquals("newValue", result.getVariableValue("NEWVAR"))
    }

    @Test
    fun `Environment should update existing variable case-insensitively`() {
        val result = testEnvironment.setVariable(
            Variable("BASEURL", "https://updated.com", "Updated URL")
        )

        // Should still have 4 variables (not 5)
        assertEquals(4, result.variables.size)

        // The new value should be found regardless of case
        assertEquals("https://updated.com", result.getVariableValue("baseUrl"))
        assertEquals("https://updated.com", result.getVariableValue("BASEURL"))
        assertEquals("https://updated.com", result.getVariableValue("BaseUrl"))

        // Verify only the matching variable was replaced
        assertEquals("test-key-123", result.getVariableValue("apiKey"))
    }

    @Test
    fun `Environment should remove variable case-insensitively`() {
        val result = testEnvironment.removeVariable("APIKEY")

        assertEquals(3, result.variables.size)
        assertNull(result.getVariableValue("apiKey"))
        assertNull(result.getVariableValue("APIKEY"))

        // Other variables should remain
        assertNotNull(result.getVariableValue("baseUrl"))
    }

    @Test
    fun `Environment should remove variable with exact case`() {
        val result = testEnvironment.removeVariable("baseUrl")

        // Should remove both baseUrl and BaseUrl (case-insensitive)
        assertEquals(2, result.variables.size)
        assertNull(result.getVariableValue("baseUrl"))
        assertNull(result.getVariableValue("BaseUrl"))

        // Other variables should remain
        assertNotNull(result.getVariableValue("apiKey"))
    }

    @Test
    fun `Environment should get variables map with only enabled variables`() {
        val map = testEnvironment.getVariablesMap()

        // Should have 2 enabled variables (timeout is disabled, and BaseUrl overwrites baseUrl)
        assertEquals(2, map.size)

        // All keys should be normalized
        assertTrue(map.containsKey("baseurl"))
        assertTrue(map.containsKey("apikey"))
        assertFalse(map.containsKey("timeout")) // disabled

        // BaseUrl should have been overwritten by the later entry
        assertEquals("https://override.test.com", map["baseurl"])
        assertEquals("test-key-123", map["apikey"])
    }

    @Test
    fun `Environment should get all variables map including disabled`() {
        val map = testEnvironment.getAllVariablesMap()

        // Should have 3 unique variables (baseUrl and BaseUrl collide)
        assertEquals(3, map.size)

        assertTrue(map.containsKey("timeout"))
        assertEquals("30000", map["timeout"])
    }

    @Test
    fun `Environment create should generate unique ID`() {
        val env1 = Environment.create("Env1")
        val env2 = Environment.create("Env2")

        assertEquals("Env1", env1.name)
        assertEquals("Env2", env2.name)
        assertTrue(env1.id != env2.id)
        assertTrue(env1.id.isNotEmpty())
        assertTrue(env2.id.isNotEmpty())
    }

    @Test
    fun `Environment create should set isGlobal flag`() {
        val globalEnv = Environment.create("Global", isGlobal = true)
        val normalEnv = Environment.create("Normal", isGlobal = false)

        assertTrue(globalEnv.isGlobal)
        assertFalse(normalEnv.isGlobal)
    }

    @Test
    fun `SerializableEnvironment should convert from domain Environment`() {
        val serializable = SerializableEnvironment.from(testEnvironment, order = 1)

        assertEquals(testEnvironment.id, serializable.id)
        assertEquals(testEnvironment.name, serializable.name)
        assertEquals(testEnvironment.isGlobal, serializable.isGlobal)
        assertEquals(1, serializable.order)
        assertEquals(testEnvironment.variables.size, serializable.variables.size)

        val firstVar = serializable.variables.first()
        assertEquals("baseUrl", firstVar.key)
        assertEquals("https://api.test.com", firstVar.value)
        assertEquals("Base URL for API", firstVar.description)
        assertTrue(firstVar.enabled)
    }

    @Test
    fun `SerializableEnvironment should convert to domain Environment`() {
        val serializable = SerializableEnvironment(
            id = "serial-1",
            name = "Serializable Env",
            variables = listOf(
                SerializableVariable("key1", "value1", "desc1"),
                SerializableVariable("key2", "value2", "desc2", enabled = false)
            ),
            isGlobal = false,
            order = 5
        )

        val domain = serializable.toEnvironment()

        assertEquals("serial-1", domain.id)
        assertEquals("Serializable Env", domain.name)
        assertFalse(domain.isGlobal)
        assertEquals(2, domain.variables.size)

        assertEquals("key1", domain.variables[0].key)
        assertEquals("value1", domain.variables[0].value)
        assertEquals("desc1", domain.variables[0].description)
        assertTrue(domain.variables[0].enabled)

        assertEquals("key2", domain.variables[1].key)
        assertEquals("value2", domain.variables[1].value)
        assertFalse(domain.variables[1].enabled)
    }

    @Test
    fun `SerializableVariable should convert from domain Variable`() {
        val variable = Variable("myKey", "myValue", "myDescription", enabled = true)
        val serializable = SerializableVariable.from(variable)

        assertEquals(variable.key, serializable.key)
        assertEquals(variable.value, serializable.value)
        assertEquals(variable.description, serializable.description)
        assertEquals(variable.enabled, serializable.enabled)
    }

    @Test
    fun `SerializableVariable should convert to domain Variable`() {
        val serializable = SerializableVariable(
            key = "serialKey",
            value = "serialValue",
            description = "serialDesc",
            enabled = false
        )

        val variable = serializable.toVariable()

        assertEquals(serializable.key, variable.key)
        assertEquals(serializable.value, variable.value)
        assertEquals(serializable.description, variable.description)
        assertEquals(serializable.enabled, variable.enabled)
    }

    @Test
    fun `Environment should handle empty variable list`() {
        val emptyEnv = Environment(id = "empty", name = "Empty")

        assertEquals(0, emptyEnv.variables.size)
        assertEquals(0, emptyEnv.getVariablesMap().size)
        assertNull(emptyEnv.getVariableValue("anything"))
        assertFalse(emptyEnv.hasVariable("anything"))
    }

    @Test
    fun `Environment should handle variable with default values`() {
        val variable = Variable("simpleVar", "value")
        assertEquals("", variable.description)
        assertTrue(variable.enabled)
    }

    @Test
    fun `Environment should set variable on empty environment`() {
        val emptyEnv = Environment(id = "empty", name = "Empty")
        val updated = emptyEnv.setVariable(Variable("key", "value"))

        assertEquals(1, updated.variables.size)
        assertEquals("value", updated.getVariableValue("key"))
    }

    @Test
    fun `Environment should preserve immutability when setting variable`() {
        val originalSize = testEnvironment.variables.size
        testEnvironment.setVariable(Variable("newKey", "newValue"))

        // Original environment should be unchanged
        assertEquals(originalSize, testEnvironment.variables.size)
        assertNull(testEnvironment.getVariableValue("newKey"))
    }

    @Test
    fun `Environment should preserve immutability when removing variable`() {
        val originalSize = testEnvironment.variables.size
        testEnvironment.removeVariable("baseUrl")

        // Original environment should be unchanged
        assertEquals(originalSize, testEnvironment.variables.size)
        assertNotNull(testEnvironment.getVariableValue("baseUrl"))
    }
}

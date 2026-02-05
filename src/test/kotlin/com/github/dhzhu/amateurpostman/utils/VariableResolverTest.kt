package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import org.junit.Assert.*
import org.junit.Test

class VariableResolverTest {

    @Test
    fun testSubstituteSingleVariable() {
        val text = "Hello {{name}}!"
        val variables = mapOf("name" to "World")
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("Hello World!", result)
    }

    @Test
    fun testSubstituteMultipleVariables() {
        val text = "{{greeting}} {{name}}, how are you?"
        val variables = mapOf(
            "greeting" to "Hello",
            "name" to "Alice"
        )
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("Hello Alice, how are you?", result)
    }

    @Test
    fun testSubstituteInUrl() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            url = "https://{{baseUrl}}/users/{{userId}}",
            headers = mapOf("Authorization" to "Bearer {{token}}"),
            body = null
        )
        val variables = mapOf(
            "baseurl" to "api.example.com",
            "userid" to "123",
            "token" to "abc123"
        )

        val result = VariableResolver.substitute(request, variables)

        assertEquals("https://api.example.com/users/123", result.url)
        assertEquals("Bearer abc123", result.headers["Authorization"])
    }

    @Test
    fun testSubstituteInHeaders() {
        val request = HttpRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/data",
            headers = mapOf(
                "X-API-Key" to "{{apiKey}}",
                "Content-Type" to "application/json"
            ),
            body = null
        )
        val variables = mapOf("apikey" to "secret-key")

        val result = VariableResolver.substitute(request, variables)

        assertEquals("secret-key", result.headers["X-API-Key"])
        assertEquals("application/json", result.headers["Content-Type"])
    }

    @Test
    fun testSubstituteInBody() {
        val request = HttpRequest(
            method = HttpMethod.POST,
            url = "https://api.example.com/users",
            headers = mapOf("Content-Type" to "application/json"),
            body = """{"name": "{{name}}", "email": "{{email}}"}"""
        )
        val variables = mapOf(
            "name" to "John Doe",
            "email" to "john@example.com"
        ) // These are already lowercase

        val result = VariableResolver.substitute(request, variables)

        assertEquals("""{"name": "John Doe", "email": "john@example.com"}""", result.body)
    }

    @Test
    fun testMissingVariableLeavesPlaceholder() {
        val text = "Hello {{missing}}!"
        val variables = mapOf("other" to "value")
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("Hello {{missing}}!", result)
    }

    @Test
    fun testEmptyVariablesMapReturnsOriginal() {
        val text = "Hello {{name}}!"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        assertEquals("Hello {{name}}!", result)
    }

    @Test
    fun testCaseInsensitiveVariableLookup() {
        val text = "{{BaseURL}}/users"
        val variables = mapOf("baseurl" to "api.example.com")
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("api.example.com/users", result)
    }

    @Test
    fun testTimestampBuiltin() {
        val text = "Timestamp: {{\$timestamp}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        assertTrue(result.matches(Regex("Timestamp: \\d+")))
    }

    @Test
    fun testTimestampWithFormat() {
        val text = "Date: {{\$timestamp:yyyy-MM-dd}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        assertTrue(result.matches(Regex("Date: \\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testUuidBuiltin() {
        val text = "UUID: {{\$uuid}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        assertTrue(result.matches(Regex("UUID: [0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun testGuidBuiltin() {
        val text = "GUID: {{\$guid}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        assertTrue(result.matches(Regex("GUID: [0-9a-f]{32}")))
        assertFalse(result.contains("-"))
    }

    @Test
    fun testRandomIntBuiltin() {
        val text = "Random: {{\$randomInt}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        assertTrue(result.matches(Regex("Random: -?\\d+")))
    }

    @Test
    fun testRandomIntRange() {
        val text = "Random: {{\$randomInt:1,100}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        val value = result.substringAfter(": ").toInt()
        assertTrue(value in 1..100)
    }

    @Test
    fun testRandomIntLength() {
        val text = "Random: {{\$randomInt:5}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        val value = result.substringAfter(": ")
        assertTrue(value.length == 5)
        assertTrue(value.all { it.isDigit() })
    }

    @Test
    fun testRandomStringBuiltin() {
        val text = "Random: {{\$randomString:10}}"
        val result = VariableResolver.substituteVariables(text, emptyMap())
        val value = result.substringAfter(": ")
        assertEquals(10, value.length)
        assertTrue(value.all { it.isLetterOrDigit() })
    }

    @Test
    fun testRecursiveVariableResolution() {
        val text = "{{final}}"
        val variables = mapOf(
            "final" to "{{middle}}",
            "middle" to "{{start}}",
            "start" to "done"
        )
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("done", result)
    }

    @Test
    fun testMaxRecursionDepth() {
        // Create a circular reference
        val text = "{{var1}}"
        val variables = mapOf(
            "var1" to "{{var2}}",
            "var2" to "{{var3}}",
            "var3" to "{{var4}}",
            "var4" to "{{var5}}",
            "var5" to "{{var6}}",
            "var6" to "{{var7}}",
            "var7" to "{{var8}}",
            "var8" to "{{var9}}",
            "var9" to "{{var10}}",
            "var10" to "{{var11}}"
        )
        val result = VariableResolver.substituteVariables(text, variables)
        // Should stop at max recursion and return the original text
        assertEquals("{{var11}}", result)
    }

    @Test
    fun testMixedVariablesAndBuiltins() {
        val text = "{{baseUrl}}/users/{{\$uuid}}"
        val variables = mapOf("baseurl" to "https://api.example.com")
        val result = VariableResolver.substituteVariables(text, variables)
        assertTrue(result.startsWith("https://api.example.com/users/"))
        assertTrue(result.matches(Regex("https://api\\.example\\.com/users/[0-9a-f-]{36}")))
    }

    @Test
    fun testExtractVariableNames() {
        val text = "{{var1}} and {{var2}} and {{\$timestamp}}"
        val names = VariableResolver.extractVariableNames(text)
        assertEquals(3, names.size)
        assertTrue(names.contains("var1"))
        assertTrue(names.contains("var2"))
        assertTrue(names.contains("\$timestamp"))
    }

    @Test
    fun testValidateVariablesWithAllPresent() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            url = "https://{{baseUrl}}/users",
            headers = mapOf("Authorization" to "Bearer {{token}}"),
            body = null
        )
        val variables = mapOf(
            "baseurl" to "api.example.com",
            "token" to "secret"
        )

        val missing = VariableResolver.validateVariables(request, variables)
        assertTrue(missing.isEmpty())
    }

    @Test
    fun testValidateVariablesWithMissing() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            url = "https://{{baseUrl}}/users",
            headers = mapOf("Authorization" to "Bearer {{token}}"),
            body = null
        )
        val variables = mapOf("baseurl" to "api.example.com")

        val missing = VariableResolver.validateVariables(request, variables)
        assertEquals(1, missing.size)
        assertTrue(missing.contains("token"))
    }

    @Test
    fun testValidateVariablesIgnoresBuiltins() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            url = "https://{{baseUrl}}/users/{{\$uuid}}",
            headers = mapOf("X-Timestamp" to "{{\$timestamp}}"),
            body = null
        )
        val variables = mapOf("baseurl" to "api.example.com")

        val missing = VariableResolver.validateVariables(request, variables)
        assertTrue(missing.isEmpty())
    }

    @Test
    fun testVariablesToMapFiltersDisabled() {
        val variables = listOf(
            com.github.dhzhu.amateurpostman.models.Variable("key1", "value1", enabled = true),
            com.github.dhzhu.amateurpostman.models.Variable("key2", "value2", enabled = false),
            com.github.dhzhu.amateurpostman.models.Variable("key3", "value3", enabled = true)
        )
        val map = VariableResolver.variablesToMap(variables)
        assertEquals(2, map.size)
        assertTrue(map.containsKey("key1"))
        assertFalse(map.containsKey("key2"))
        assertTrue(map.containsKey("key3"))
    }

    @Test
    fun testGetVariableValueFromList() {
        val variables = listOf(
            com.github.dhzhu.amateurpostman.models.Variable("API_KEY", "secret1", enabled = true),
            com.github.dhzhu.amateurpostman.models.Variable("api_key", "secret2", enabled = false)
        )

        // Debug: check if variables list is correct
        assertEquals(2, variables.size)
        assertEquals("API_KEY", variables[0].key)
        assertTrue(variables[0].enabled)

        // The issue: "API_KEY" lowercases to "api_key", but "apiKey" lowercases to "apikey"
        // They don't match! Let's use the correct search key
        assertEquals("api_key", variables[0].key.lowercase())

        // Debug: check the comparison with correct key
        val searchKey = "API_KEY"  // Use exact match or use "api_key"
        val normalizedSearchKey = searchKey.lowercase()
        assertEquals("api_key", normalizedSearchKey)

        // Check if the first variable should match
        val shouldMatch = variables[0].key.lowercase() == normalizedSearchKey && variables[0].enabled
        assertTrue(shouldMatch)

        // Now test the actual function
        val value = VariableResolver.getVariableValue(variables, "API_KEY")
        assertNotNull(value, "getVariableValue should find the variable")
        assertEquals("secret1", value)
    }

    @Test
    fun testGetVariableValueReturnsNullForDisabled() {
        val variables = listOf(
            com.github.dhzhu.amateurpostman.models.Variable("key", "value", enabled = false)
        )
        val value = VariableResolver.getVariableValue(variables, "key")
        assertNull(value)
    }

    @Test
    fun testSubstitutePreservesNonVariableContent() {
        val text = "This is {{var}} and this is normal text"
        val variables = mapOf("var" to "a variable")
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("This is a variable and this is normal text", result)
    }

    @Test
    fun testEmptyRequestBody() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            url = "https://{{baseUrl}}/users",
            headers = emptyMap(),
            body = null
        )
        val variables = mapOf("baseurl" to "api.example.com")
        val result = VariableResolver.substitute(request, variables)
        assertNull(result.body)
    }

    @Test
    fun testSubstituteWithSpacesInBraces() {
        val text = "Value: {{ var }}"
        val variables = mapOf("var" to "test")
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("Value: test", result)
    }

    @Test
    fun testMultipleSubstitutionsOfSameVariable() {
        val text = "{{name}} is friends with {{name}}"
        val variables = mapOf("name" to "Alice")
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("Alice is friends with Alice", result)
    }

    @Test
    fun testNestedVariablesWithFinalValue() {
        val text = "{{outer}}"
        val variables = mapOf(
            "outer" to "{{middle}}123",
            "middle" to "inner_"
        )
        val result = VariableResolver.substituteVariables(text, variables)
        assertEquals("inner_123", result)
    }
}

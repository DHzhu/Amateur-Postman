package com.github.dhzhu.amateurpostman.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GraphQLRequestTest {

    @Test
    fun `test GraphQLRequest toJson creates valid JSON`() {
        val request = GraphQLRequest(
            query = "{ user(id: 1) { name email } }",
            operationName = null,
            variables = null
        )

        val json = request.toJson()
        assertTrue(json.contains("\"query\""))
        assertTrue(json.contains("{ user(id: 1) { name email } }"))
    }

    @Test
    fun `test GraphQLRequest toJson with variables`() {
        val request = GraphQLRequest(
            query = "query GetUser(\$id: ID!) { user(id: \$id) { name } }",
            operationName = "GetUser",
            variables = "{\"id\": \"123\"}"
        )

        val json = request.toJson()
        assertTrue(json.contains("\"query\""))
        assertTrue(json.contains("\"operationName\""))
        assertTrue(json.contains("\"variables\""))
        assertTrue(json.contains("GetUser"))
    }

    @Test
    fun `test GraphQLRequest toJson without operation name`() {
        val request = GraphQLRequest(
            query = "{ user { name } }",
            operationName = null,
            variables = "{\"limit\": 10}"
        )

        val json = request.toJson()
        assertTrue(json.contains("\"query\""))
        assertFalse(json.contains("\"operationName\""))
        assertTrue(json.contains("\"variables\""))
    }

    @Test
    fun `test GraphQLRequest fromJson parses simple query`() {
        val json = """{"query":"{ user { name } }"}"""
        val request = GraphQLRequest.fromJson(json)

        assertNotNull(request)
        assertEquals("{ user { name } }", request!!.query)
        assertNull(request.operationName)
        assertNull(request.variables)
    }

    @Test
    fun `test GraphQLRequest fromJson parses full request`() {
        val json = """{"query":"query GetUser(${'$'}id: ID!) { user(id: ${'$'}id) { name } }","operationName":"GetUser","variables":{"id":"123"}}"""
        val request = GraphQLRequest.fromJson(json)

        assertNotNull(request)
        assertTrue(request!!.query.contains("GetUser"))
        assertEquals("GetUser", request.operationName)
        assertNotNull(request.variables)
    }

    @Test
    fun `test GraphQLRequest fromJson returns null for invalid JSON`() {
        val request = GraphQLRequest.fromJson("not valid json")
        assertNull(request)
    }

    @Test
    fun `test GraphQLRequest fromJson returns null for JSON without query`() {
        val json = """{"variables":{"id":"123"}}"""
        val request = GraphQLRequest.fromJson(json)
        assertNull(request)
    }

    @Test
    fun `test GraphQLRequest toJson and fromJson roundtrip`() {
        val original = GraphQLRequest(
            query = "query User(${'$'}id: ID!) { user(id: ${'$'}id) { name email } }",
            operationName = "User",
            variables = "{\"id\": \"456\"}"
        )

        val json = original.toJson()
        val restored = GraphQLRequest.fromJson(json)

        assertNotNull(restored)
        assertEquals(original.query, restored!!.query)
        assertEquals(original.operationName, restored.operationName)
        assertNotNull(restored.variables)
    }

    @Test
    fun `test GraphQLRequest with complex variables`() {
        val request = GraphQLRequest(
            query = "mutation CreateUser(${'$'}input: UserInput!) { createUser(input: ${'$'}input) { id } }",
            operationName = "CreateUser",
            variables = """{"input":{"name":"John","email":"john@example.com","age":30}}"""
        )

        val json = request.toJson()
        assertTrue(json.contains("createUser"))
        assertTrue(json.contains("John"))
    }

    @Test
    fun `test GraphQLRequest with empty variables string`() {
        val request = GraphQLRequest(
            query = "{ user { name } }",
            operationName = null,
            variables = ""
        )

        val json = request.toJson()
        // Empty variables should not be included
        assertFalse(json.contains("\"variables\""))
    }

    @Test
    fun `test GraphQLRequest with blank variables string`() {
        val request = GraphQLRequest(
            query = "{ user { name } }",
            operationName = null,
            variables = "   "
        )

        val json = request.toJson()
        // Blank variables should not be included
        assertFalse(json.contains("\"variables\""))
    }
}

package com.github.dhzhu.amateurpostman.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonServiceTest {

    // ── Serialization ──────────────────────────────────────────────────────────

    @Test
    fun `serialize simple data class to JSON`() {
        data class Person(val name: String, val age: Int)
        val json = JsonService.mapper.writeValueAsString(Person("Alice", 30))
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"Alice\""))
        assertTrue(json.contains("\"age\""))
        assertTrue(json.contains("30"))
    }

    @Test
    fun `serialize produces pretty-printed JSON`() {
        data class Item(val id: Int, val value: String)
        val json = JsonService.mapper.writeValueAsString(Item(1, "test"))
        // Pretty printing inserts newlines
        assertTrue(json.contains("\n"), "Expected indented output")
    }

    @Test
    fun `serialize null field as JSON null`() {
        data class WithNullable(val key: String, val value: String?)
        val json = JsonService.mapper.writeValueAsString(WithNullable("k", null))
        assertTrue(json.contains("null"))
    }

    // ── Deserialization ────────────────────────────────────────────────────────

    @Test
    fun `deserialize JSON to Kotlin data class`() {
        data class Point(val x: Double, val y: Double)
        val point = JsonService.mapper.readValue("""{"x":1.5,"y":2.5}""", Point::class.java)
        assertEquals(1.5, point.x)
        assertEquals(2.5, point.y)
    }

    @Test
    fun `deserialize with default values honors Kotlin defaults`() {
        data class Config(val host: String = "localhost", val port: Int = 8080)
        // Only provide host, port should use Kotlin default
        val config = JsonService.mapper.readValue("""{"host":"example.com"}""", Config::class.java)
        assertEquals("example.com", config.host)
        assertEquals(8080, config.port)
    }

    @Test
    fun `deserialize ignores unknown properties`() {
        data class Simple(val name: String)
        // JSON has extra field not in the data class — should not throw
        assertDoesNotThrow {
            val obj = JsonService.mapper.readValue("""{"name":"test","extra":"ignored"}""", Simple::class.java)
            assertEquals("test", obj.name)
        }
    }

    @Test
    fun `deserialize null JSON field into nullable Kotlin property`() {
        data class WithNullable(val key: String, val value: String?)
        val obj = JsonService.mapper.readValue("""{"key":"k","value":null}""", WithNullable::class.java)
        assertNull(obj.value)
    }

    // ── Compact mapper ─────────────────────────────────────────────────────────

    @Test
    fun `compact mapper serializes without newlines`() {
        data class Item(val id: Int)
        val json = JsonService.compactMapper.writeValueAsString(Item(1))
        assertFalse(json.contains("\n"), "Compact mapper should not produce indented output")
    }

    // ── readTree / JsonNode ─────────────────────────────────────────────────────

    @Test
    fun `readTree parses JSON object node`() {
        val node = JsonService.mapper.readTree("""{"key":"value"}""")
        assertTrue(node.isObject)
        assertEquals("value", node.get("key").asText())
    }

    @Test
    fun `readTree parses JSON array node`() {
        val node = JsonService.mapper.readTree("""[1,2,3]""")
        assertTrue(node.isArray)
        assertEquals(3, node.size())
    }
}

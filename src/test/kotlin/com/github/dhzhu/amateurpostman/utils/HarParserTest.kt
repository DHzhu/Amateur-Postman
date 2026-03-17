package com.github.dhzhu.amateurpostman.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HarParserTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `parse minimal valid HAR with one GET entry`() {
        val json = minimalHar("GET", "https://api.example.com/users")

        val result = HarParser.parseFromJson(json)

        assertTrue(result is HarParser.HarParseResult.Success)
        val log = (result as HarParser.HarParseResult.Success).log
        assertEquals(1, log.entries.size)
        assertEquals("GET", log.entries[0].request.method)
        assertEquals("https://api.example.com/users", log.entries[0].request.url)
    }

    @Test
    fun `parse HAR with request headers`() {
        val json = """
            {
              "log": {
                "version": "1.2",
                "entries": [{
                  "request": {
                    "method": "POST",
                    "url": "https://api.example.com/data",
                    "headers": [
                      {"name": "Content-Type", "value": "application/json"},
                      {"name": "Authorization", "value": "Bearer token123"}
                    ]
                  },
                  "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "application/json"}}
                }]
              }
            }
        """.trimIndent()

        val log = parseSuccess(json)
        val headers = log.entries[0].request.headers
        assertEquals(2, headers.size)
        assertEquals("application/json", headers.find { it.name == "Content-Type" }?.value)
        assertEquals("Bearer token123", headers.find { it.name == "Authorization" }?.value)
    }

    @Test
    fun `parse HAR with JSON postData`() {
        val json = """
            {
              "log": {
                "entries": [{
                  "request": {
                    "method": "POST",
                    "url": "https://api.example.com/users",
                    "postData": {
                      "mimeType": "application/json",
                      "text": "{\"name\":\"Alice\"}"
                    }
                  },
                  "response": {"status": 201, "statusText": "Created", "content": {"mimeType": "application/json"}}
                }]
              }
            }
        """.trimIndent()

        val log = parseSuccess(json)
        val postData = log.entries[0].request.postData
        assertNotNull(postData)
        assertEquals("application/json", postData!!.mimeType)
        assertEquals("{\"name\":\"Alice\"}", postData.text)
    }

    @Test
    fun `parse HAR with multipart postData params`() {
        val json = """
            {
              "log": {
                "entries": [{
                  "request": {
                    "method": "POST",
                    "url": "https://api.example.com/upload",
                    "postData": {
                      "mimeType": "multipart/form-data",
                      "params": [
                        {"name": "username", "value": "alice"},
                        {"name": "file", "value": "", "fileName": "photo.jpg", "contentType": "image/jpeg"}
                      ]
                    }
                  },
                  "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "application/json"}}
                }]
              }
            }
        """.trimIndent()

        val log = parseSuccess(json)
        val params = log.entries[0].request.postData?.params
        assertNotNull(params)
        assertEquals(2, params!!.size)
        assertEquals("username", params[0].name)
        assertEquals("alice", params[0].value)
        assertEquals("photo.jpg", params[1].fileName)
        assertEquals("image/jpeg", params[1].contentType)
    }

    @Test
    fun `parse HAR with Chrome _resourceType field`() {
        val json = """
            {
              "log": {
                "entries": [
                  {
                    "_resourceType": "xhr",
                    "request": {"method": "GET", "url": "https://api.example.com/data"},
                    "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "application/json"}}
                  },
                  {
                    "_resourceType": "image",
                    "request": {"method": "GET", "url": "https://cdn.example.com/logo.png"},
                    "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "image/png"}}
                  }
                ]
              }
            }
        """.trimIndent()

        val log = parseSuccess(json)
        assertEquals(2, log.entries.size)
        assertEquals("xhr", log.entries[0]._resourceType)
        assertEquals("image", log.entries[1]._resourceType)
    }

    @Test
    fun `parse HAR with multiple entries from different hosts`() {
        val json = """
            {
              "log": {
                "entries": [
                  {
                    "request": {"method": "GET", "url": "https://api.example.com/users"},
                    "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "application/json"}}
                  },
                  {
                    "request": {"method": "POST", "url": "https://auth.example.com/login"},
                    "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "application/json"}}
                  }
                ]
              }
            }
        """.trimIndent()

        val log = parseSuccess(json)
        assertEquals(2, log.entries.size)
        assertEquals("https://api.example.com/users", log.entries[0].request.url)
        assertEquals("https://auth.example.com/login", log.entries[1].request.url)
    }

    @Test
    fun `parse empty entries list`() {
        val json = """{"log": {"version": "1.2", "entries": []}}"""

        val result = HarParser.parseFromJson(json)

        assertTrue(result is HarParser.HarParseResult.Success)
        val log = (result as HarParser.HarParseResult.Success).log
        assertTrue(log.entries.isEmpty())
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    fun `return failure for invalid JSON`() {
        val result = HarParser.parseFromJson("{ not valid json }")

        assertTrue(result is HarParser.HarParseResult.Failure)
        val msg = (result as HarParser.HarParseResult.Failure).message
        assertTrue(msg.contains("Invalid JSON", ignoreCase = true))
    }

    @Test
    fun `return failure for empty string`() {
        val result = HarParser.parseFromJson("")

        assertTrue(result is HarParser.HarParseResult.Failure)
    }

    @Test
    fun `return failure for missing log field`() {
        val result = HarParser.parseFromJson("{\"notLog\": {}}")

        assertTrue(result is HarParser.HarParseResult.Failure)
    }

    // ── File-based parsing ────────────────────────────────────────────────────

    @Test
    fun `parseFromFile reads and parses correctly`() {
        val file = createTempHarFile(minimalHar("DELETE", "https://api.example.com/item/1"))

        val result = HarParser.parseFromFile(file)

        assertTrue(result is HarParser.HarParseResult.Success)
        val log = (result as HarParser.HarParseResult.Success).log
        assertEquals("DELETE", log.entries[0].request.method)

        file.delete()
    }

    @Test
    fun `parseFromFile returns failure for non-existent file`() {
        val result = HarParser.parseFromFile(java.io.File("/tmp/nonexistent_har_test_file.har"))

        assertTrue(result is HarParser.HarParseResult.Failure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseSuccess(json: String): HarParser.HarLog {
        val result = HarParser.parseFromJson(json)
        assertTrue(result is HarParser.HarParseResult.Success, "Expected Success but got: $result")
        return (result as HarParser.HarParseResult.Success).log
    }

    private fun minimalHar(method: String, url: String) = """
        {
          "log": {
            "version": "1.2",
            "entries": [{
              "request": {"method": "$method", "url": "$url"},
              "response": {"status": 200, "statusText": "OK", "content": {"mimeType": "application/json"}}
            }]
          }
        }
    """.trimIndent()

    private fun createTempHarFile(content: String): java.io.File {
        val file = java.io.File.createTempFile("test", ".har")
        file.writeText(content)
        return file
    }
}

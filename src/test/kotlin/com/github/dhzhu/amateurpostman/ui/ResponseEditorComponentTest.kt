package com.github.dhzhu.amateurpostman.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for ResponseEditorComponent utility logic.
 *
 * Note: The IntelliJ Editor component itself cannot be instantiated in headless
 * unit tests. These tests cover the pure-logic companion object methods
 * (file-type detection, truncation, size formatting).
 */
class ResponseEditorComponentTest {

    // ── detectFileExtension ───────────────────────────────────────────────────

    @Test
    fun `detectFileExtension returns json for application-json`() {
        assertEquals("json", ResponseEditorComponent.detectFileExtension("application/json"))
    }

    @Test
    fun `detectFileExtension returns json for application-json with charset`() {
        assertEquals("json", ResponseEditorComponent.detectFileExtension("application/json; charset=utf-8"))
    }

    @Test
    fun `detectFileExtension returns xml for text-xml`() {
        assertEquals("xml", ResponseEditorComponent.detectFileExtension("text/xml"))
    }

    @Test
    fun `detectFileExtension returns xml for application-xml`() {
        assertEquals("xml", ResponseEditorComponent.detectFileExtension("application/xml"))
    }

    @Test
    fun `detectFileExtension returns html for text-html`() {
        assertEquals("html", ResponseEditorComponent.detectFileExtension("text/html"))
    }

    @Test
    fun `detectFileExtension returns txt for unknown content type`() {
        assertEquals("txt", ResponseEditorComponent.detectFileExtension("text/plain"))
    }

    @Test
    fun `detectFileExtension returns txt for empty content type`() {
        assertEquals("txt", ResponseEditorComponent.detectFileExtension(""))
    }

    @Test
    fun `detectFileExtension is case insensitive`() {
        assertEquals("json", ResponseEditorComponent.detectFileExtension("Application/JSON"))
        assertEquals("xml", ResponseEditorComponent.detectFileExtension("TEXT/XML"))
        assertEquals("html", ResponseEditorComponent.detectFileExtension("TEXT/HTML"))
    }

    // ── truncateForDisplay ────────────────────────────────────────────────────

    @Test
    fun `truncateForDisplay returns content unchanged if under limit`() {
        val content = "hello world"
        val (result, truncated) = ResponseEditorComponent.truncateForDisplay(content, maxBytes = 1000)
        assertEquals(content, result)
        assertFalse(truncated)
    }

    @Test
    fun `truncateForDisplay truncates content when over limit`() {
        val content = "a".repeat(200)
        val (result, truncated) = ResponseEditorComponent.truncateForDisplay(content, maxBytes = 100)
        assertTrue(result.length <= 100)
        assertTrue(truncated)
    }

    @Test
    fun `truncateForDisplay returns truncated=true when truncation occurs`() {
        val content = "x".repeat(1000)
        val (_, truncated) = ResponseEditorComponent.truncateForDisplay(content, maxBytes = 100)
        assertTrue(truncated)
    }

    @Test
    fun `truncateForDisplay returns truncated=false for empty string`() {
        val (result, truncated) = ResponseEditorComponent.truncateForDisplay("", maxBytes = 100)
        assertEquals("", result)
        assertFalse(truncated)
    }

    // ── formatSize ────────────────────────────────────────────────────────────

    @Test
    fun `formatSize formats bytes correctly`() {
        assertEquals("512 B", ResponseEditorComponent.formatSize(512))
    }

    @Test
    fun `formatSize formats KB correctly`() {
        val result = ResponseEditorComponent.formatSize(2048)
        assertTrue(result.contains("KB"), "Expected KB in '$result'")
    }

    @Test
    fun `formatSize formats MB correctly`() {
        val result = ResponseEditorComponent.formatSize(3 * 1024 * 1024)
        assertTrue(result.contains("MB"), "Expected MB in '$result'")
    }

    @Test
    fun `formatSize boundary 1023 bytes shows B`() {
        assertTrue(ResponseEditorComponent.formatSize(1023).contains("B"))
        assertFalse(ResponseEditorComponent.formatSize(1023).contains("KB"))
    }

    @Test
    fun `formatSize boundary 1024 bytes shows KB`() {
        assertTrue(ResponseEditorComponent.formatSize(1024).contains("KB"))
    }
}

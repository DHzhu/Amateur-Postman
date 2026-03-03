package com.github.dhzhu.amateurpostman.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Unit tests for SyntaxHighlighter */
class SyntaxHighlighterTest {

    @Test
    fun testHighlightSimpleJsonObject() {
        val json = """{"name":"John"}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        assertTrue(tokens.isNotEmpty())

        // Should have key token
        assertTrue(tokens.any { it.type == SyntaxHighlighter.TokenType.KEY })
        // Should have string token
        assertTrue(tokens.any { it.type == SyntaxHighlighter.TokenType.STRING })
        // Should have bracket tokens
        assertTrue(tokens.any { it.type == SyntaxHighlighter.TokenType.BRACKET })
    }

    @Test
    fun testHighlightJsonWithNumber() {
        val json = """{"age":30}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        assertTrue(tokens.any { it.type == SyntaxHighlighter.TokenType.NUMBER })
    }

    @Test
    fun testHighlightJsonWithBoolean() {
        val json = """{"active":true}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        assertTrue(tokens.any { it.type == SyntaxHighlighter.TokenType.BOOLEAN })
    }

    @Test
    fun testHighlightJsonWithNull() {
        val json = """{"value":null}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        assertTrue(tokens.any { it.type == SyntaxHighlighter.TokenType.NULL })
    }

    @Test
    fun testHighlightJsonArray() {
        val json = """[1,2,3]"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        // Should have bracket tokens for [ and ]
        val bracketTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.BRACKET }
        assertTrue(bracketTokens.size >= 2)

        // Should have number tokens
        val numberTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.NUMBER }
        assertEquals(3, numberTokens.size)
    }

    @Test
    fun testHighlightNestedJson() {
        val json = """{"user":{"name":"John","age":30}}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        // Should have multiple key tokens
        val keyTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.KEY }
        assertTrue(keyTokens.size >= 2)
    }

    @Test
    fun testHighlightJsonWithDecimalNumber() {
        val json = """{"price":19.99}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        val numberTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.NUMBER }
        assertTrue(numberTokens.any { it.text == "19.99" })
    }

    @Test
    fun testHighlightJsonWithNegativeNumber() {
        val json = """{"temperature":-5}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        val numberTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.NUMBER }
        assertTrue(numberTokens.any { it.text == "-5" })
    }

    @Test
    fun testHighlightJsonWithScientificNotation() {
        val json = """{"value":1.5e10}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        val numberTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.NUMBER }
        assertTrue(numberTokens.any { it.text == "1.5e10" })
    }

    @Test
    fun testHighlightJsonWithEscapedString() {
        val json = """{"message":"Hello \"World\""}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        val stringTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.STRING }
        assertTrue(stringTokens.isNotEmpty())
    }

    @Test
    fun testFormatJsonPrettyPrint() {
        val json = """{"name":"John","age":30}"""
        val formatted = SyntaxHighlighter.formatJson(json)

        // Should contain newlines after formatting
        assertTrue(formatted.contains("\n"))
        // Should contain indentation
        assertTrue(formatted.contains("  "))
    }

    @Test
    fun testFormatInvalidJsonReturnsOriginal() {
        val invalid = "not valid json"
        val formatted = SyntaxHighlighter.formatJson(invalid)

        assertEquals(invalid, formatted)
    }

    @Test
    fun testIsValidJsonWithObject() {
        assertTrue(SyntaxHighlighter.isValidJson("""{"key":"value"}"""))
    }

    @Test
    fun testIsValidJsonWithArray() {
        assertTrue(SyntaxHighlighter.isValidJson("""[1,2,3]"""))
    }

    @Test
    fun testIsValidJsonWithWhitespace() {
        assertTrue(SyntaxHighlighter.isValidJson("""  {"key":"value"}  """))
    }

    @Test
    fun testIsNotValidJson() {
        assertFalse(SyntaxHighlighter.isValidJson("plain text"))
        assertFalse(SyntaxHighlighter.isValidJson("<html>"))
    }

    @Test
    fun testHighlightPreservesWhitespace() {
        val json = """{
    "name": "John"
}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        // Should have whitespace tokens
        val whitespaceTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.WHITESPACE }
        assertTrue(whitespaceTokens.isNotEmpty())
    }

    @Test
    fun testHighlightJsonWithFalseBoolean() {
        val json = """{"enabled":false}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        val boolTokens = tokens.filter { it.type == SyntaxHighlighter.TokenType.BOOLEAN }
        assertTrue(boolTokens.any { it.text == "false" })
    }

    @Test
    fun testTokensReconstructOriginalJson() {
        val json = """{"name":"John","age":30}"""
        val tokens = SyntaxHighlighter.highlightJsonToTokens(json)

        val reconstructed = tokens.joinToString("") { it.text }
        assertEquals(json, reconstructed)
    }
}

package com.github.dhzhu.amateurpostman.utils

import java.awt.Color
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/** Syntax highlighter for JSON and other response formats */
object SyntaxHighlighter {

    // Color scheme (dark theme friendly)
    private val KEY_COLOR = Color(156, 220, 254) // Light blue for keys
    private val STRING_COLOR = Color(206, 145, 120) // Orange for strings
    private val NUMBER_COLOR = Color(181, 206, 168) // Green for numbers
    private val BOOLEAN_COLOR = Color(86, 156, 214) // Blue for booleans
    private val NULL_COLOR = Color(86, 156, 214) // Blue for null
    private val BRACKET_COLOR = Color(212, 212, 212) // Gray for brackets
    private val COLON_COLOR = Color(212, 212, 212) // Gray for colons
    private val DEFAULT_COLOR = Color(212, 212, 212) // Default text color

    // Performance threshold: use simplified highlighting for large files
    const val SIZE_THRESHOLD_FOR_SIMPLIFIED = 500_000 // 500KB

    /**
     * Apply JSON syntax highlighting to a JTextPane.
     *
     * @deprecated Use [com.github.dhzhu.amateurpostman.ui.ResponseEditorComponent] instead,
     * which leverages the IntelliJ native Editor for better performance on large responses.
     */
    @Deprecated("Use ResponseEditorComponent with IntelliJ Editor instead")
    fun highlightJson(textPane: JTextPane, json: String) {
        val doc = textPane.styledDocument

        // Clear existing content
        doc.remove(0, doc.length)

        val tokens = tokenizeJson(json)

        for (token in tokens) {
            val style = createStyle(token.type)
            doc.insertString(doc.length, token.text, style)
        }
    }

    /**
     * Apply JSON syntax highlighting and return as styled document
     * @param json The JSON string to highlight
     * @return List of styled tokens for rendering
     */
    fun highlightJsonToTokens(json: String): List<StyledToken> {
        // Use simplified tokenization for large files
        return if (json.length > SIZE_THRESHOLD_FOR_SIMPLIFIED) {
            tokenizeJsonSimplified(json)
        } else {
            tokenizeJson(json)
        }
    }

    /**
     * Simplified tokenizer for large JSON files - faster but less detailed.
     * Groups consecutive similar characters together.
     */
    private fun tokenizeJsonSimplified(json: String): List<StyledToken> {
        val tokens = mutableListOf<StyledToken>()
        val length = json.length
        var i = 0

        // Use StringBuilder for better memory efficiency
        val currentToken = StringBuilder()

        while (i < length) {
            val c = json[i]
            when {
                c == '"' -> {
                    // Flush current token
                    if (currentToken.isNotEmpty()) {
                        tokens.add(StyledToken(currentToken.toString(), TokenType.DEFAULT))
                        currentToken.clear()
                    }

                    // Read entire string
                    val start = i
                    i++
                    while (i < length && json[i] != '"') {
                        if (json[i] == '\\' && i + 1 < length) {
                            i += 2
                        } else {
                            i++
                        }
                    }
                    if (i < length) i++
                    tokens.add(StyledToken(json.substring(start, i), TokenType.STRING))
                }
                c == '{' || c == '}' || c == '[' || c == ']' -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(StyledToken(currentToken.toString(), TokenType.DEFAULT))
                        currentToken.clear()
                    }
                    tokens.add(StyledToken(c.toString(), TokenType.BRACKET))
                    i++
                }
                c == ':' || c == ',' -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(StyledToken(currentToken.toString(), TokenType.DEFAULT))
                        currentToken.clear()
                    }
                    tokens.add(StyledToken(c.toString(), TokenType.COLON))
                    i++
                }
                c.isWhitespace() -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(StyledToken(currentToken.toString(), TokenType.DEFAULT))
                        currentToken.clear()
                    }
                    val start = i
                    while (i < length && json[i].isWhitespace()) i++
                    tokens.add(StyledToken(json.substring(start, i), TokenType.WHITESPACE))
                }
                else -> {
                    currentToken.append(c)
                    i++
                }
            }
        }

        // Flush remaining token
        if (currentToken.isNotEmpty()) {
            tokens.add(StyledToken(currentToken.toString(), TokenType.DEFAULT))
        }

        return tokens
    }

    /** Tokenize JSON string into styled tokens */
    private fun tokenizeJson(json: String): List<StyledToken> {
        val tokens = mutableListOf<StyledToken>()
        var i = 0
        val length = json.length
        var expectKey = true // Track if we're expecting a key in an object

        while (i < length) {
            val c = json[i]
            when {
                c.isWhitespace() -> {
                    val start = i
                    while (i < length && json[i].isWhitespace()) {
                        i++
                    }
                    tokens.add(StyledToken(json.substring(start, i), TokenType.WHITESPACE))
                }
                c == '"' -> {
                    val start = i
                    i++ // Skip opening quote
                    while (i < length && json[i] != '"') {
                        if (json[i] == '\\' && i + 1 < length) {
                            i += 2 // Skip escape sequence
                        } else {
                            i++
                        }
                    }
                    if (i < length) i++ // Skip closing quote
                    val text = json.substring(start, i)

                    // Determine if this is a key or value
                    val isKey = expectKey && lookAheadForColon(json, i)
                    val type = if (isKey) TokenType.KEY else TokenType.STRING
                    tokens.add(StyledToken(text, type))

                    if (isKey) {
                        expectKey = false
                    }
                }
                c == ':' -> {
                    tokens.add(StyledToken(":", TokenType.COLON))
                    expectKey = false
                    i++
                }
                c == ',' -> {
                    tokens.add(StyledToken(",", TokenType.COMMA))
                    expectKey = true
                    i++
                }
                c == '{' -> {
                    tokens.add(StyledToken("{", TokenType.BRACKET))
                    expectKey = true
                    i++
                }
                c == '}' -> {
                    tokens.add(StyledToken("}", TokenType.BRACKET))
                    i++
                }
                c == '[' -> {
                    tokens.add(StyledToken("[", TokenType.BRACKET))
                    expectKey = false
                    i++
                }
                c == ']' -> {
                    tokens.add(StyledToken("]", TokenType.BRACKET))
                    i++
                }
                c == '-' || c.isDigit() -> {
                    val start = i
                    // Parse number
                    if (c == '-') i++
                    while (i < length && json[i].isDigit()) i++
                    if (i < length && json[i] == '.') {
                        i++
                        while (i < length && json[i].isDigit()) i++
                    }
                    if (i < length && (json[i] == 'e' || json[i] == 'E')) {
                        i++
                        if (i < length && (json[i] == '+' || json[i] == '-')) i++
                        while (i < length && json[i].isDigit()) i++
                    }
                    tokens.add(StyledToken(json.substring(start, i), TokenType.NUMBER))
                }
                json.substring(i).startsWith("true") -> {
                    tokens.add(StyledToken("true", TokenType.BOOLEAN))
                    i += 4
                }
                json.substring(i).startsWith("false") -> {
                    tokens.add(StyledToken("false", TokenType.BOOLEAN))
                    i += 5
                }
                json.substring(i).startsWith("null") -> {
                    tokens.add(StyledToken("null", TokenType.NULL))
                    i += 4
                }
                else -> {
                    tokens.add(StyledToken(c.toString(), TokenType.DEFAULT))
                    i++
                }
            }
        }
        return tokens
    }

    private fun lookAheadForColon(json: String, startIndex: Int): Boolean {
        var i = startIndex
        while (i < json.length && json[i].isWhitespace()) {
            i++
        }
        return i < json.length && json[i] == ':'
    }

    private fun createStyle(type: TokenType): SimpleAttributeSet {
        val style = SimpleAttributeSet()
        val color =
                when (type) {
                    TokenType.KEY -> KEY_COLOR
                    TokenType.STRING -> STRING_COLOR
                    TokenType.NUMBER -> NUMBER_COLOR
                    TokenType.BOOLEAN -> BOOLEAN_COLOR
                    TokenType.NULL -> NULL_COLOR
                    TokenType.BRACKET, TokenType.COLON, TokenType.COMMA -> BRACKET_COLOR
                    else -> DEFAULT_COLOR
                }
        StyleConstants.setForeground(style, color)
        return style
    }

    /** Format JSON with proper indentation (pretty print) */
    fun formatJson(json: String): String {
        return try {
            val node = com.github.dhzhu.amateurpostman.services.JsonService.mapper.readTree(json)
            com.github.dhzhu.amateurpostman.services.JsonService.mapper.writeValueAsString(node)
        } catch (e: Exception) {
            json
        }
    }

    /** Check if a string appears to be valid JSON */
    fun isValidJson(str: String): Boolean {
        val trimmed = str.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }

    data class StyledToken(val text: String, val type: TokenType)

    enum class TokenType {
        KEY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        BRACKET,
        COLON,
        COMMA,
        WHITESPACE,
        DEFAULT
    }
}

package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.utils.SyntaxHighlighter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * High-performance response viewer with adaptive rendering for large responses.
 *
 * Features:
 * - Automatic syntax highlighting for JSON responses
 * - Performance-adaptive mode: disables highlighting for large files (>5MB)
 * - Chunked rendering for very large responses
 * - User toggle for syntax highlighting
 * - Light/Dark theme support
 */
class HighPerfResponseViewer : JPanel(BorderLayout()) {

    companion object {
        private const val SIZE_THRESHOLD_FOR_WARNING = 5 * 1024 * 1024 // 5MB
        private const val SIZE_THRESHOLD_FOR_CHUNKING = 10 * 1024 * 1024 // 10MB
        private const val CHUNK_SIZE = 100_000 // 100KB chunks

        // Theme-aware colors
        private val BACKGROUND_COLOR = JBColor(Color(43, 43, 43), Color(255, 255, 255))
        private val FOREGROUND_COLOR = JBColor(Color(212, 212, 212), Color(50, 50, 50))
        private val WARNING_BG_COLOR = JBColor(Color(60, 60, 40), Color(255, 255, 200))
        private val WARNING_FG_COLOR = JBColor(Color(255, 200, 100), Color(150, 100, 0))

        // JSON syntax colors (theme-aware)
        private val KEY_COLOR = JBColor(Color(156, 220, 254), Color(0, 100, 180))
        private val STRING_COLOR = JBColor(Color(206, 145, 120), Color(180, 80, 40))
        private val NUMBER_COLOR = JBColor(Color(181, 206, 168), Color(0, 128, 0))
        private val BOOLEAN_COLOR = JBColor(Color(86, 156, 214), Color(0, 80, 160))
        private val NULL_COLOR = JBColor(Color(86, 156, 214), Color(0, 80, 160))
        private val DEFAULT_COLOR = JBColor(Color(212, 212, 212), Color(50, 50, 50))
    }

    private val textPane = JTextPane()
    private val scrollPane = JBScrollPane(textPane)

    // State
    private var currentBody: String = ""
    private var isJsonContent: Boolean = false
    private var syntaxHighlightingEnabled: Boolean = true
    private var isLargeFile: Boolean = false

    // UI components for warning
    private val warningPanel = JPanel(BorderLayout())
    private val warningLabel = JLabel()
    private val toggleHighlightButton = JButton("Enable Highlighting")

    init {
        setupUI()
    }

    private fun setupUI() {
        border = JBUI.Borders.empty()

        // Configure text pane with theme-aware colors
        textPane.isEditable = false
        textPane.font = Font("Monospaced", Font.PLAIN, 12)
        textPane.background = BACKGROUND_COLOR
        textPane.foreground = FOREGROUND_COLOR

        // Setup warning panel with theme-aware colors
        warningPanel.border = JBUI.Borders.empty(5)
        warningPanel.background = WARNING_BG_COLOR
        warningLabel.foreground = WARNING_FG_COLOR
        warningLabel.text = "Large response detected. Syntax highlighting disabled for performance."
        warningLabel.icon = UIManager.getIcon("OptionPane.warningIcon")

        toggleHighlightButton.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                toggleSyntaxHighlighting()
            }
        })

        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        buttonPanel.isOpaque = false
        buttonPanel.add(toggleHighlightButton)

        warningPanel.add(warningLabel, BorderLayout.CENTER)
        warningPanel.add(buttonPanel, BorderLayout.EAST)

        add(scrollPane, BorderLayout.CENTER)
    }

    /**
     * Sets the response body content with automatic format detection and performance optimization.
     *
     * @param body The response body text
     * @param contentType The content type header value
     */
    fun setResponseBody(body: String, contentType: String) {
        currentBody = body
        isJsonContent = contentType.contains("application/json", ignoreCase = true)

        val bodySize = body.toByteArray().size
        isLargeFile = bodySize > SIZE_THRESHOLD_FOR_WARNING

        // Reset state
        removeWarningPanel()

        if (isLargeFile) {
            // Large file - check if we need chunking
            if (bodySize > SIZE_THRESHOLD_FOR_CHUNKING) {
                displayChunkedContent(body, contentType)
            } else {
                // Show warning and option to enable highlighting
                displayWithWarning(body, contentType)
            }
        } else {
            // Normal file - apply syntax highlighting if JSON
            if (isJsonContent && syntaxHighlightingEnabled) {
                displayWithHighlighting(body)
            } else {
                displayPlainText(body)
            }
        }
    }

    private fun displayWithHighlighting(body: String) {
        val doc = textPane.styledDocument
        doc.remove(0, doc.length)

        try {
            val formattedBody = try {
                SyntaxHighlighter.formatJson(body)
            } catch (e: Exception) {
                body
            }

            val tokens = SyntaxHighlighter.highlightJsonToTokens(formattedBody)
            for (token in tokens) {
                val style = SimpleAttributeSet()
                val color = when (token.type) {
                    SyntaxHighlighter.TokenType.KEY -> KEY_COLOR
                    SyntaxHighlighter.TokenType.STRING -> STRING_COLOR
                    SyntaxHighlighter.TokenType.NUMBER -> NUMBER_COLOR
                    SyntaxHighlighter.TokenType.BOOLEAN, SyntaxHighlighter.TokenType.NULL -> BOOLEAN_COLOR
                    else -> DEFAULT_COLOR
                }
                StyleConstants.setForeground(style, color)
                doc.insertString(doc.length, token.text, style)
            }
        } catch (e: Exception) {
            // Fallback to plain text on error
            displayPlainText(body)
        }
    }

    private fun displayPlainText(body: String) {
        val doc = textPane.styledDocument
        doc.remove(0, doc.length)

        val style = SimpleAttributeSet()
        StyleConstants.setForeground(style, FOREGROUND_COLOR)
        doc.insertString(0, body, style)
    }

    private fun displayWithWarning(body: String, contentType: String) {
        // Show warning panel
        add(warningPanel, BorderLayout.NORTH)
        revalidate()
        repaint()

        // Display plain text initially
        displayPlainText(body)

        // Update button text based on current state
        toggleHighlightButton.text = if (syntaxHighlightingEnabled) "Disable Highlighting" else "Enable Highlighting"
    }

    private fun displayChunkedContent(body: String, contentType: String) {
        // For very large files, we display in a read-only editor with virtual scrolling
        // For now, just display as plain text with a warning
        add(warningPanel, BorderLayout.NORTH)
        revalidate()
        repaint()

        displayPlainText(body)

        warningLabel.text = "Very large response (${formatSize(body.length)}). Displaying as plain text for performance."
        toggleHighlightButton.isVisible = false
    }

    private fun toggleSyntaxHighlighting() {
        syntaxHighlightingEnabled = !syntaxHighlightingEnabled

        if (syntaxHighlightingEnabled && isJsonContent) {
            // Re-apply highlighting
            displayWithHighlighting(currentBody)
            toggleHighlightButton.text = "Disable Highlighting"
        } else {
            // Remove highlighting
            displayPlainText(currentBody)
            toggleHighlightButton.text = "Enable Highlighting"
        }
    }

    private fun removeWarningPanel() {
        remove(warningPanel)
        revalidate()
        repaint()
    }

    /**
     * Clears the response content.
     */
    fun clear() {
        currentBody = ""
        isJsonContent = false
        isLargeFile = false

        removeWarningPanel()

        val doc = textPane.styledDocument
        doc.remove(0, doc.length)
    }

    /**
     * Gets the current response text.
     */
    fun getText(): String = currentBody

    /**
     * Returns whether syntax highlighting is currently enabled.
     */
    fun isHighlightingEnabled(): Boolean = syntaxHighlightingEnabled

    /**
     * Sets whether syntax highlighting is enabled.
     */
    fun setHighlightingEnabled(enabled: Boolean) {
        syntaxHighlightingEnabled = enabled
        if (currentBody.isNotEmpty()) {
            setResponseBody(currentBody, if (isJsonContent) "application/json" else "text/plain")
        }
    }

    private fun formatSize(chars: Int): String {
        // Approximate byte size for display (assuming UTF-8)
        val bytes = chars * 2 // Rough estimate
        return when {
            bytes < 1024 -> "$chars chars"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
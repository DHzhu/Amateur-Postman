package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.utils.SyntaxHighlighter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.ListCellRenderer
import javax.swing.ListModel
import javax.swing.SwingUtilities
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/**
 * High-performance message list for streaming protocols (WebSocket, gRPC).
 *
 * Features:
 * - JSON syntax highlighting for message content
 * - Automatic message count limiting to prevent memory issues
 * - Efficient rendering with custom cell renderer
 * - Support for both incoming and outgoing messages
 */
class StreamMessageList<T : StreamMessage>(
    private val maxMessages: Int = 1000
) : JPanel(BorderLayout()) {

    private val model = DefaultListModel<T>()
    private val list: JList<T> = JList(model)
    private val renderer = MessageCellRenderer()

    // Theme-aware colors
    private val outgoingColor = JBColor(Color(0, 100, 0), Color(98, 150, 85))
    private val incomingColor = JBColor(Color(0, 0, 139), Color(86, 156, 214))
    private val selectionBackground = JBColor(Color(75, 110, 175), Color(75, 110, 175))

    init {
        border = JBUI.Borders.empty()
        list.cellRenderer = renderer
        list.font = Font("Monospaced", Font.PLAIN, 12)
        list.selectionBackground = selectionBackground
        list.fixedCellHeight = -1 // Variable height

        add(JBScrollPane(list), BorderLayout.CENTER)
    }

    /**
     * Adds a message to the list.
     * If the message count exceeds maxMessages, removes the oldest messages.
     */
    fun addMessage(message: T) {
        SwingUtilities.invokeLater {
            // Remove old messages if exceeding limit
            while (model.size() >= maxMessages) {
                model.removeElementAt(0)
            }
            model.addElement(message)
            // Auto-scroll to bottom
            list.ensureIndexIsVisible(model.size() - 1)
        }
    }

    /**
     * Clears all messages.
     */
    fun clearMessages() {
        SwingUtilities.invokeLater {
            model.clear()
        }
    }

    /**
     * Returns the current message count.
     */
    fun getMessageCount(): Int = model.size()

    /**
     * Returns all messages as a list.
     */
    fun getMessages(): List<T> = (0 until model.size()).map { model.getElementAt(it) }

    /**
     * Sets a custom model (for advanced use cases).
     */
    fun setModel(newModel: ListModel<T>) {
        list.model = newModel
    }

    // ─── Cell Renderer with Syntax Highlighting ────────────────────────────────

    private inner class MessageCellRenderer : JPanel(BorderLayout()), ListCellRenderer<T> {
        private val headerLabel = JLabel()
        private val contentPane = JTextPane()

        init {
            border = JBUI.Borders.empty(4, 8)
            layout = BorderLayout(0, 2)

            headerLabel.font = Font("Monospaced", Font.BOLD, 11)
            add(headerLabel, BorderLayout.NORTH)

            contentPane.isEditable = false
            contentPane.font = Font("Monospaced", Font.PLAIN, 12)
            contentPane.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
            add(contentPane, BorderLayout.CENTER)
        }

        override fun getListCellRendererComponent(
            list: JList<out T>?,
            value: T?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): JPanel {
            if (value == null) return this

            // Header: direction icon + timestamp
            val icon = if (value.isOutgoing) ArrowIcon(true) else ArrowIcon(false)
            headerLabel.icon = icon
            headerLabel.text = formatTimestamp(value.timestamp)
            headerLabel.foreground = if (value.isOutgoing) outgoingColor else incomingColor

            // Content with syntax highlighting
            val content = value.content
            if (SyntaxHighlighter.isValidJson(content)) {
                highlightJson(content)
            } else {
                setPlainText(content)
            }

            // Selection background
            background = if (isSelected) list?.selectionBackground ?: selectionBackground
                        else list?.background ?: Color.WHITE
            contentPane.background = background

            isOpaque = true
            return this
        }

        private fun highlightJson(json: String) {
            val doc = contentPane.styledDocument
            doc.remove(0, doc.length)

            try {
                // Format JSON for readability
                val formatted = SyntaxHighlighter.formatJson(json)
                val tokens = SyntaxHighlighter.highlightJsonToTokens(formatted)

                // Theme-aware colors
                val keyColor = JBColor(Color(156, 220, 254), Color(86, 156, 214))
                val stringColor = JBColor(Color(206, 145, 120), Color(206, 145, 120))
                val numberColor = JBColor(Color(181, 206, 168), Color(181, 206, 168))
                val booleanColor = JBColor(Color(86, 156, 214), Color(86, 156, 214))
                val defaultColor = JBColor(Color(212, 212, 212), Color(188, 188, 188))

                for (token in tokens) {
                    val style = SimpleAttributeSet()
                    val color = when (token.type) {
                        SyntaxHighlighter.TokenType.KEY -> keyColor
                        SyntaxHighlighter.TokenType.STRING -> stringColor
                        SyntaxHighlighter.TokenType.NUMBER -> numberColor
                        SyntaxHighlighter.TokenType.BOOLEAN, SyntaxHighlighter.TokenType.NULL -> booleanColor
                        else -> defaultColor
                    }
                    StyleConstants.setForeground(style, color)
                    doc.insertString(doc.length, token.text, style)
                }
            } catch (e: Exception) {
                setPlainText(json)
            }
        }

        private fun setPlainText(text: String) {
            val doc = contentPane.styledDocument
            doc.remove(0, doc.length)
            val style = SimpleAttributeSet()
            StyleConstants.setForeground(style, JBColor(Color(212, 212, 212), Color(188, 188, 188)))
            doc.insertString(0, text, style)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("HH:mm:ss.SSS")
            return format.format(date)
        }
    }

    // ─── Arrow Icon ─────────────────────────────────────────────────────────────

    private class ArrowIcon(private val isOutgoing: Boolean) : Icon {
        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
            g?.color = if (isOutgoing) Color(0, 120, 0) else Color(0, 0, 160)
            if (isOutgoing) {
                // Right arrow (outgoing)
                val px = intArrayOf(x + 2, x + 10, x + 6)
                val py = intArrayOf(y + 4, y + 7, y + 10)
                g?.fillPolygon(px, py, 3)
            } else {
                // Left arrow (incoming)
                val px = intArrayOf(x + 10, x + 2, x + 6)
                val py = intArrayOf(y + 4, y + 7, y + 10)
                g?.fillPolygon(px, py, 3)
            }
        }

        override fun getIconWidth(): Int = 12
        override fun getIconHeight(): Int = 14
    }
}

/**
 * Interface for stream messages (WebSocket, gRPC).
 * Implement this in your message data classes.
 */
interface StreamMessage {
    val content: String
    val isOutgoing: Boolean
    val timestamp: Long
}
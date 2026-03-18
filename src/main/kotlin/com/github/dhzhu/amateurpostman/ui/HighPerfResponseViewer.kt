package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.services.JsonService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager

/**
 * High-performance response viewer backed by IntelliJ's native [Editor] component.
 *
 * Features:
 * - Native syntax highlighting for JSON, XML, HTML (via IntelliJ language support)
 * - Virtual scrolling and code folding out-of-the-box
 * - Pretty-printing for JSON responses
 * - Graceful truncation for responses > 10 MB with a user-visible notice
 * - Proper resource management via [Disposable]
 */
class HighPerfResponseViewer(
    project: Project,
    parentDisposable: Disposable
) : JPanel(BorderLayout()), Disposable {

    companion object {
        private val WARNING_BG = JBColor(Color(60, 60, 40), Color(255, 255, 200))
        private val WARNING_FG = JBColor(Color(255, 200, 100), Color(150, 100, 0))
    }

    private val editorComponent = ResponseEditorComponent(project, this)

    // Warning banner shown when a response is truncated
    private val warningPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(4, 8)
        background = WARNING_BG
        isVisible = false
    }
    private val warningLabel = JLabel().apply {
        foreground = WARNING_FG
        icon = UIManager.getIcon("OptionPane.warningIcon")
    }

    // Public state accessible for tests / parent panels
    var currentBody: String = ""; private set
    var currentContentType: String = ""; private set

    init {
        border = JBUI.Borders.empty()
        warningPanel.add(warningLabel, BorderLayout.CENTER)
        add(warningPanel, BorderLayout.NORTH)
        add(editorComponent.component, BorderLayout.CENTER)
        Disposer.register(parentDisposable, this)
    }

    /**
     * Displays [body] with syntax highlighting inferred from [contentType].
     * JSON bodies are pretty-printed automatically. Responses larger than
     * [ResponseEditorComponent.TRUNCATION_THRESHOLD] bytes are truncated.
     */
    fun setResponseBody(body: String, contentType: String) {
        currentBody = body
        currentContentType = contentType

        val isJson = contentType.contains("application/json", ignoreCase = true)
        val displayText = if (isJson) prettyPrintJson(body) else body

        val (truncated, wasTruncated) = ResponseEditorComponent.truncateForDisplay(displayText)

        if (wasTruncated) {
            val originalBytes = body.toByteArray(Charsets.UTF_8).size.toLong()
            warningLabel.text =
                "Response truncated for display. Full size: ${ResponseEditorComponent.formatSize(originalBytes)}"
            warningPanel.isVisible = true
        } else {
            warningPanel.isVisible = false
        }

        editorComponent.setContent(truncated, contentType)
        revalidate()
        repaint()
    }

    /** Clears the viewer. */
    fun clear() {
        currentBody = ""
        currentContentType = ""
        warningPanel.isVisible = false
        editorComponent.clear()
    }

    /** Returns the raw (untruncated) response body. */
    fun getText(): String = currentBody

    override fun dispose() {
        // editorComponent is registered with this as parentDisposable; Disposer handles it.
    }

    private fun prettyPrintJson(json: String): String = try {
        val node = JsonService.mapper.readTree(json)
        JsonService.mapper.writeValueAsString(node)
    } catch (_: Exception) {
        json
    }
}

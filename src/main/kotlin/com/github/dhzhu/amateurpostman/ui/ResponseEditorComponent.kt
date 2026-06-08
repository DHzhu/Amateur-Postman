package com.github.dhzhu.amateurpostman.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Wraps an IntelliJ read-only [Editor] for displaying HTTP response bodies.
 *
 * Provides:
 * - Automatic syntax highlighting based on Content-Type
 * - Native virtual scrolling and code folding
 * - Proper Disposable lifecycle — release the editor when [parentDisposable] is disposed
 */
class ResponseEditorComponent(
    private val project: Project,
    parentDisposable: Disposable
) : Disposable {

    companion object {
        /** Responses larger than this (bytes) will be truncated before display. */
        const val TRUNCATION_THRESHOLD = 10 * 1024 * 1024 // 10 MB

        /**
         * Maps an HTTP Content-Type header value to a file extension used for
         * syntax highlighting. Returns "txt" for unrecognised types.
         */
        fun detectFileExtension(contentType: String): String = when {
            contentType.contains("application/json", ignoreCase = true) -> "json"
            contentType.contains("text/xml", ignoreCase = true) ||
                contentType.contains("application/xml", ignoreCase = true) -> "xml"
            contentType.contains("text/html", ignoreCase = true) -> "html"
            else -> "txt"
        }

        /**
         * Truncates [content] to at most [maxBytes] UTF-8 bytes.
         *
         * @return Pair(displayText, wasTruncated)
         */
        fun truncateForDisplay(content: String, maxBytes: Int = TRUNCATION_THRESHOLD): Pair<String, Boolean> {
            val bytes = content.toByteArray(Charsets.UTF_8)
            if (bytes.size <= maxBytes) return Pair(content, false)
            // Find the longest prefix whose UTF-8 encoding fits in maxBytes
            var end = maxBytes
            // Back up past any partial multi-byte sequence (continuation bytes 10xxxxxx)
            while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) end--
            // Also remove the leading byte of the now-incomplete character
            if (end > 0) end--
            val truncated = String(bytes, 0, end, Charsets.UTF_8)
            return Pair(truncated, true)
        }

        /** Human-readable byte size string (B / KB / MB). */
        fun formatSize(byteCount: Long): String = when {
            byteCount < 1024 -> "$byteCount B"
            byteCount < 1024 * 1024 -> String.format("%.1f KB", byteCount / 1024.0)
            else -> String.format("%.1f MB", byteCount / (1024.0 * 1024.0))
        }
    }

    private val document = EditorFactory.getInstance().createDocument("")
    private var currentEditor: Editor? = null
    private var currentFileType: FileType? = null
    private val wrapper = JPanel(BorderLayout())

    /** The Swing component to embed in layouts. */
    val component: JComponent get() = wrapper

    init {
        createEditor(PlainTextFileType.INSTANCE)
        Disposer.register(parentDisposable, this)
    }

    private fun createEditor(fileType: FileType) {
        currentEditor?.let { old ->
            wrapper.remove(old.component)
            EditorFactory.getInstance().releaseEditor(old)
        }

        val editor = EditorFactory.getInstance().createEditor(document, project, fileType, true)
        configureEditor(editor)
        wrapper.add(editor.component, BorderLayout.CENTER)
        wrapper.revalidate()
        wrapper.repaint()

        currentEditor = editor
        currentFileType = fileType
    }

    private fun configureEditor(editor: Editor) {
        editor.settings.apply {
            isLineNumbersShown = false
            isFoldingOutlineShown = true
            isLineMarkerAreaShown = false
            isIndentGuidesShown = false
            additionalLinesCount = 0
            isCaretRowShown = false
            isVirtualSpace = false
        }
    }

    /**
     * Replaces the editor content and applies syntax highlighting for [contentType].
     */
    fun setContent(text: String, contentType: String = "text/plain") {
        val extension = detectFileExtension(contentType)
        val fileType = try {
            FileTypeManager.getInstance().getFileTypeByExtension(extension)
        } catch (_: Exception) {
            PlainTextFileType.INSTANCE
        }

        if (fileType != currentFileType) {
            createEditor(fileType)
        }

        ApplicationManager.getApplication().runWriteAction {
            document.setReadOnly(false)
            document.setText(text.replace("\r\n", "\n"))
            document.setReadOnly(true)
        }
    }

    /** Clears editor content. */
    fun clear() {
        ApplicationManager.getApplication().runWriteAction {
            document.setReadOnly(false)
            document.setText("")
            document.setReadOnly(true)
        }
    }

    override fun dispose() {
        currentEditor?.let { EditorFactory.getInstance().releaseEditor(it) }
        currentEditor = null
    }
}

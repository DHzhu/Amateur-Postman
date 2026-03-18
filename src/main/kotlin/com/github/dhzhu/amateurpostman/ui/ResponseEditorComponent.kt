package com.github.dhzhu.amateurpostman.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

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
            return if (bytes.size > maxBytes) {
                Pair(content.take(maxBytes), true)
            } else {
                Pair(content, false)
            }
        }

        /** Human-readable byte size string (B / KB / MB). */
        fun formatSize(byteCount: Long): String = when {
            byteCount < 1024 -> "$byteCount B"
            byteCount < 1024 * 1024 -> String.format("%.1f KB", byteCount / 1024.0)
            else -> String.format("%.1f MB", byteCount / (1024.0 * 1024.0))
        }
    }

    private val document = EditorFactory.getInstance().createDocument("")
    val editor: EditorEx = EditorFactory.getInstance()
        .createViewer(document, project, EditorKind.PREVIEW) as EditorEx

    /** The Swing component to embed in layouts. */
    val component: JComponent get() = editor.component

    init {
        configureEditor()
        Disposer.register(parentDisposable, this)
    }

    private fun configureEditor() {
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
        try {
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension)
                ?: PlainTextFileType.INSTANCE
            editor.highlighter = EditorHighlighterFactory.getInstance()
                .createEditorHighlighter(project, fileType)
        } catch (_: Exception) {
            // Highlighting is best-effort; content still displayed without it.
        }

        document.setReadOnly(false)
        document.setText(text)
        document.setReadOnly(true)
    }

    /** Clears editor content. */
    fun clear() {
        document.setReadOnly(false)
        document.setText("")
        document.setReadOnly(true)
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}

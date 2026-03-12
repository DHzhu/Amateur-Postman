package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.utils.OpenApiImporter
import com.github.dhzhu.amateurpostman.utils.OpenApiParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.AlignX
import java.awt.Dimension
import java.io.File
import javax.swing.*

/**
 * Dialog for importing collections from OpenAPI specifications.
 *
 * Supports:
 * - Import from local file (JSON/YAML)
 * - Import from URL
 */
class OpenApiImportDialog(private val project: Project) {

    private var selectedSource: ImportSource = ImportSource.FILE
    private val fileField = JBTextField(30)
    private val urlField = JBTextField(30)

    fun showAndGet(): OpenApiImportResult? {
        val fileChooserButton = JButton("Browse...")

        fileChooserButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Select OpenAPI Specification"
            fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "OpenAPI Specification (*.json, *.yaml, *.yml)", "json", "yaml", "yml"
            )

            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileField.text = fileChooser.selectedFile.absolutePath
            }
        }

        val result = Messages.showDialog(
            project,
            """
                <html>
                <body style='width: 400px; padding: 10px;'>
                <h3>Import from OpenAPI Specification</h3>
                <p>Import API definitions from OpenAPI 3.x or Swagger 2.0 specifications.</p>
                <br>
                <p><b>From File:</b> Select a local JSON or YAML file</p>
                <p><b>From URL:</b> Enter the URL to an OpenAPI specification</p>
                </body>
                </html>
            """.trimIndent(),
            "Import OpenAPI",
            arrayOf("Import", "Cancel"),
            0,
            Messages.getQuestionIcon(),
            null
        )

        if (result != 0) return null

        // Show input dialog
        return showImportInputDialog()
    }

    private fun showImportInputDialog(): OpenApiImportResult? {
        val options = arrayOf("From File", "From URL", "Cancel")
        val choice = Messages.showDialog(
            project,
            "Choose import source:",
            "Import OpenAPI",
            options,
            0,
            Messages.getQuestionIcon()
        )

        return when (choice) {
            0 -> importFromFile()
            1 -> importFromUrl()
            else -> null
        }
    }

    private fun importFromFile(): OpenApiImportResult? {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Select OpenAPI Specification"
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "OpenAPI Specification (*.json, *.yaml, *.yml)", "json", "yaml", "yml"
        )

        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null
        }

        val file = fileChooser.selectedFile

        // Parse and import
        val parseResult = OpenApiParser.parseFromFile(file)
        if (!parseResult.isSuccess) {
            Messages.showErrorDialog(
                project,
                "Failed to parse OpenAPI specification:\n${parseResult.error}",
                "Parse Error"
            )
            return null
        }

        val importResult = OpenApiImporter.importFromParseResult(parseResult)
        if (!importResult.isSuccess) {
            Messages.showErrorDialog(
                project,
                "Failed to import OpenAPI:\n${importResult.error}",
                "Import Error"
            )
            return null
        }

        return OpenApiImportResult(
            collection = importResult.collection!!,
            warnings = importResult.warnings,
            sourcePath = file.absolutePath
        )
    }

    private fun importFromUrl(): OpenApiImportResult? {
        val url = Messages.showInputDialog(
            project,
            "Enter OpenAPI specification URL:",
            "Import from URL",
            Messages.getQuestionIcon(),
            "",
            null
        ) ?: return null

        if (url.isBlank()) {
            Messages.showErrorDialog(project, "URL cannot be empty.", "Invalid URL")
            return null
        }

        // Parse and import
        val parseResult = OpenApiParser.parseFromUrl(url)
        if (!parseResult.isSuccess) {
            Messages.showErrorDialog(
                project,
                "Failed to parse OpenAPI specification from URL:\n${parseResult.error}",
                "Parse Error"
            )
            return null
        }

        val importResult = OpenApiImporter.importFromParseResult(parseResult)
        if (!importResult.isSuccess) {
            Messages.showErrorDialog(
                project,
                "Failed to import OpenAPI:\n${importResult.error}",
                "Import Error"
            )
            return null
        }

        return OpenApiImportResult(
            collection = importResult.collection!!,
            warnings = importResult.warnings,
            sourcePath = url
        )
    }

    /**
     * Source type for import.
     */
    enum class ImportSource {
        FILE, URL
    }

    /**
     * Result of the import operation.
     */
    data class OpenApiImportResult(
        val collection: com.github.dhzhu.amateurpostman.models.RequestCollection,
        val warnings: List<String>,
        val sourcePath: String
    )
}
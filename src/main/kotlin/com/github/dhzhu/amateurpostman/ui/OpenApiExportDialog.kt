package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.utils.OpenApiExporter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * Dialog for configuring and triggering an OpenAPI 3.0 export.
 *
 * Presents a format selector (YAML / JSON) and a toggle for including
 * sensitive headers (Authorization, Cookie, etc.) in the export.
 * After the user clicks Export, the caller should invoke [getSelectedFormat]
 * and [includeSensitiveHeaders] to drive the export call.
 */
class OpenApiExportDialog(
    project: Project,
    private val collectionName: String
) : DialogWrapper(project) {

    private val yamlRadio = JRadioButton("YAML  (.yaml)", true)
    private val jsonRadio = JRadioButton("JSON  (.json)", false)
    private val sensitiveHeadersCheckbox = JCheckBox("Include sensitive headers (Authorization, Cookie…)", false)

    init {
        title = "Export \"$collectionName\" as OpenAPI 3.0"
        setOKButtonText("Export…")
        ButtonGroup().apply {
            add(yamlRadio)
            add(jsonRadio)
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
        }

        // Row 0 — format label
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        panel.add(JBLabel("Output format:"), gbc)

        // Row 1 — YAML radio
        gbc.gridy = 1; gbc.gridwidth = 1
        panel.add(yamlRadio, gbc)

        // Row 2 — JSON radio
        gbc.gridy = 2
        panel.add(jsonRadio, gbc)

        // Row 3 — sensitive headers checkbox
        gbc.gridy = 3; gbc.gridwidth = 2
        gbc.insets = JBUI.insets(8, 8, 4, 8)
        panel.add(sensitiveHeadersCheckbox, gbc)

        return panel
    }

    fun getSelectedFormat(): OpenApiExporter.ExportFormat =
        if (yamlRadio.isSelected) OpenApiExporter.ExportFormat.YAML
        else OpenApiExporter.ExportFormat.JSON

    val includeSensitiveHeaders: Boolean
        get() = sensitiveHeadersCheckbox.isSelected
}

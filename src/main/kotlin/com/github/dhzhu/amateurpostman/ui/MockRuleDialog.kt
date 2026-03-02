package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.BodyMatcher
import com.github.dhzhu.amateurpostman.models.BodyMatchMode
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * Dialog for creating or editing a Mock Rule.
 */
class MockRuleDialog(
    private val project: Project,
    private val existingRule: MockRule?
) : DialogWrapper(project) {

    private val pathField = JTextField(30)
    private val methodCombo = JComboBox(HttpMethod.entries.toTypedArray())
    private val statusCodeField = JSpinner(SpinnerNumberModel(200, 100, 599, 1))
    private val delayField = JSpinner(SpinnerNumberModel(0, 0, 60000, 100))
    private val enabledCheckbox = JCheckBox("Enabled", true)
    private val bodyTextArea = JBTextArea(10, 40)
    private val headersTextArea = JBTextArea(5, 40)

    // Body matching components
    private val bodyMatchModeCombo = JComboBox(BodyMatchMode.entries.toTypedArray())
    private val bodyMatchPatternArea = JBTextArea(3, 40)

    init {
        title = if (existingRule == null) "Add Mock Rule" else "Edit Mock Rule"
        init()
        populateFields()
    }

    private fun populateFields() {
        existingRule?.let { rule ->
            pathField.text = rule.path
            methodCombo.selectedItem = rule.method
            statusCodeField.value = rule.statusCode
            delayField.value = rule.delayMs.toInt()
            enabledCheckbox.isSelected = rule.enabled
            bodyTextArea.text = rule.body
            headersTextArea.text = rule.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }

            // Body matching
            bodyMatchModeCombo.selectedItem = rule.bodyMatcher.mode
            bodyMatchPatternArea.text = rule.bodyMatcher.pattern
        }
    }

    override fun createCenterPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        var row = 0

        // Path
        gbc.gridx = 0
        gbc.gridy = row
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5)
        panel.add(JBLabel("Path:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(pathField, gbc)

        // Method
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        panel.add(JBLabel("Method:"), gbc)

        gbc.gridx = 1
        panel.add(methodCombo, gbc)

        // Status Code
        row++
        gbc.gridx = 0
        gbc.gridy = row
        panel.add(JBLabel("Status Code:"), gbc)

        gbc.gridx = 1
        panel.add(statusCodeField, gbc)

        // Delay
        row++
        gbc.gridx = 0
        gbc.gridy = row
        panel.add(JBLabel("Delay (ms):"), gbc)

        gbc.gridx = 1
        panel.add(delayField, gbc)

        // Enabled
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        panel.add(enabledCheckbox, gbc)

        // Request Body Matching
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weighty = 0.0

        val bodyMatchPanel = JPanel(GridBagLayout())
        bodyMatchPanel.border = BorderFactory.createTitledBorder("Request Body Matching (optional)")
        val gbcInner = GridBagConstraints()

        gbcInner.gridx = 0
        gbcInner.gridy = 0
        gbcInner.anchor = GridBagConstraints.WEST
        gbcInner.insets = JBUI.insets(2)
        bodyMatchPanel.add(JBLabel("Match Mode:"), gbcInner)

        gbcInner.gridx = 1
        gbcInner.fill = GridBagConstraints.HORIZONTAL
        gbcInner.weightx = 1.0
        bodyMatchPanel.add(bodyMatchModeCombo, gbcInner)

        gbcInner.gridx = 0
        gbcInner.gridy = 1
        gbcInner.gridwidth = 2
        gbcInner.fill = GridBagConstraints.BOTH
        gbcInner.weighty = 1.0
        gbcInner.insets = JBUI.insets(5, 2, 2, 2)
        val patternScrollPane = JBScrollPane(bodyMatchPatternArea)
        bodyMatchPatternArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        patternScrollPane.preferredSize = Dimension(350, 60)
        bodyMatchPanel.add(patternScrollPane, gbcInner)

        // Add helper text
        gbcInner.gridx = 0
        gbcInner.gridy = 2
        gbcInner.weighty = 0.0
        gbcInner.fill = GridBagConstraints.HORIZONTAL
        bodyMatchPanel.add(JBLabel("<html><small>NONE: ignore body | EXACT: exact match | CONTAINS: substring | REGEX: pattern</small></html>"), gbcInner)

        panel.add(bodyMatchPanel, gbc)

        // Response Body
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 1.0
        val bodyScrollPane = JBScrollPane(bodyTextArea)
        bodyScrollPane.border = BorderFactory.createTitledBorder("Response Body (JSON)")
        bodyScrollPane.preferredSize = Dimension(400, 150)
        panel.add(bodyScrollPane, gbc)

        // Response Headers
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weighty = 0.5
        val headersScrollPane = JBScrollPane(headersTextArea)
        headersScrollPane.border = BorderFactory.createTitledBorder("Response Headers (Key: Value per line)")
        headersScrollPane.preferredSize = Dimension(400, 80)
        panel.add(headersScrollPane, gbc)

        return panel
    }

    /**
     * Gets the MockRule from the dialog inputs.
     */
    fun getMockRule(): MockRule {
        val headers = parseHeaders(headersTextArea.text)
        val bodyMatcher = BodyMatcher(
            mode = bodyMatchModeCombo.selectedItem as BodyMatchMode,
            pattern = bodyMatchPatternArea.text
        )

        return MockRule(
            id = existingRule?.id ?: java.util.UUID.randomUUID().toString(),
            path = pathField.text.trim(),
            method = methodCombo.selectedItem as HttpMethod,
            statusCode = statusCodeField.value as Int,
            headers = headers,
            body = bodyTextArea.text,
            delayMs = (delayField.value as Int).toLong(),
            enabled = enabledCheckbox.isSelected,
            bodyMatcher = bodyMatcher
        )
    }

    private fun parseHeaders(text: String): Map<String, String> {
        if (text.isBlank()) return emptyMap()

        return text.lines()
            .filter { it.contains(":") }
            .associate { line ->
                val parts = line.split(":", limit = 2)
                parts[0].trim() to parts.getOrNull(1)?.trim().orEmpty()
            }
            .filterValues { it.isNotEmpty() }
    }

    override fun doValidate(): ValidationInfo? {
        if (pathField.text.isNullOrBlank()) {
            return ValidationInfo("Path is required", pathField)
        }
        if (!pathField.text.startsWith("/")) {
            return ValidationInfo("Path must start with /", pathField)
        }

        // Validate regex pattern if REGEX mode is selected
        if (bodyMatchModeCombo.selectedItem == BodyMatchMode.REGEX && bodyMatchPatternArea.text.isNotBlank()) {
            try {
                Regex(bodyMatchPatternArea.text)
            } catch (e: Exception) {
                return ValidationInfo("Invalid regex pattern: ${e.message}", bodyMatchPatternArea)
            }
        }

        return null
    }
}
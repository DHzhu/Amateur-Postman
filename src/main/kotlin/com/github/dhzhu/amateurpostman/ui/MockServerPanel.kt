package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.MockRule
import com.github.dhzhu.amateurpostman.services.MockServerManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Panel for managing Mock Server rules and server lifecycle.
 *
 * Features:
 * - Server start/stop controls with port configuration
 * - Rules table with Path, Method, Status, Body columns
 * - Add/Edit/Delete rule buttons
 * - Enable/disable individual rules
 */
class MockServerPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val mockServerManager = project.service<MockServerManager>()
    private val cs = CoroutineScope(Dispatchers.Swing)

    // UI Components
    private val statusLabel = JBLabel("Server: Stopped")
    private val portField = JSpinner(SpinnerNumberModel(8080, 1024, 65535, 1))
    private val startButton = JButton("Start")
    private val stopButton = JButton("Stop").apply { isEnabled = false }

    private val rulesTableModel = MockRulesTableModel()
    private val rulesTable = JBTable(rulesTableModel)
    private val addRuleButton = JButton("Add Rule")
    private val editRuleButton = JButton("Edit")
    private val deleteRuleButton = JButton("Delete")
    private val toggleRuleButton = JButton("Toggle")

    init {
        createUI()
        setupListeners()
        loadRules()
    }

    private fun createUI() {
        border = JBUI.Borders.empty(10)

        // Top panel - Server controls
        val topPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5)
        topPanel.add(statusLabel, gbc)

        gbc.gridx = 1
        topPanel.add(JBLabel("Port:"), gbc)

        gbc.gridx = 2
        portField.preferredSize = JBUI.size(80, 25)
        topPanel.add(portField, gbc)

        gbc.gridx = 3
        topPanel.add(startButton, gbc)

        gbc.gridx = 4
        topPanel.add(stopButton, gbc)

        add(topPanel, BorderLayout.NORTH)

        // Center panel - Rules table
        val centerPanel = JPanel(BorderLayout())
        centerPanel.border = BorderFactory.createTitledBorder("Mock Rules")

        setupRulesTable()
        val scrollPane = JBScrollPane(rulesTable)
        centerPanel.add(scrollPane, BorderLayout.CENTER)

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(addRuleButton)
        buttonPanel.add(editRuleButton)
        buttonPanel.add(deleteRuleButton)
        buttonPanel.add(toggleRuleButton)
        centerPanel.add(buttonPanel, BorderLayout.SOUTH)

        add(centerPanel, BorderLayout.CENTER)
    }

    private fun setupRulesTable() {
        // Column widths
        rulesTable.columnModel.getColumn(0).preferredWidth = 50   // Enabled
        rulesTable.columnModel.getColumn(1).preferredWidth = 200  // Path
        rulesTable.columnModel.getColumn(2).preferredWidth = 70   // Method
        rulesTable.columnModel.getColumn(3).preferredWidth = 50   // Status
        rulesTable.columnModel.getColumn(4).preferredWidth = 100  // Body Match
        rulesTable.columnModel.getColumn(5).preferredWidth = 250  // Response Preview
        rulesTable.columnModel.getColumn(6).preferredWidth = 70   // Delay
        rulesTable.rowHeight = 25

        // Center align status code column
        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = SwingConstants.CENTER
        rulesTable.columnModel.getColumn(3).cellRenderer = centerRenderer

        // Selection listener
        rulesTable.selectionModel.addListSelectionListener {
            updateButtonStates()
        }
    }

    private fun setupListeners() {
        // Start server
        startButton.addActionListener {
            cs.launch {
                val port = portField.value as Int
                val actualPort = mockServerManager.start(port)
                if (actualPort != null) {
                    updateServerStatus(true, actualPort)
                } else {
                    Messages.showErrorDialog(
                        project,
                        "Failed to start Mock Server on port $port",
                        "Server Error"
                    )
                }
            }
        }

        // Stop server
        stopButton.addActionListener {
            mockServerManager.stop()
            updateServerStatus(false, null)
        }

        // Add rule
        addRuleButton.addActionListener {
            showRuleDialog(null)
        }

        // Edit rule
        editRuleButton.addActionListener {
            val selectedRow = rulesTable.selectedRow
            if (selectedRow >= 0) {
                val rule = rulesTableModel.getRuleAt(selectedRow)
                showRuleDialog(rule)
            }
        }

        // Delete rule
        deleteRuleButton.addActionListener {
            val selectedRow = rulesTable.selectedRow
            if (selectedRow >= 0) {
                val result = Messages.showYesNoDialog(
                    project,
                    "Delete this mock rule?",
                    "Confirm Delete",
                    Messages.getQuestionIcon()
                )
                if (result == Messages.YES) {
                    val rule = rulesTableModel.getRuleAt(selectedRow)
                    mockServerManager.removeRule(rule.id)
                    loadRules()
                }
            }
        }

        // Toggle rule enabled state
        toggleRuleButton.addActionListener {
            val selectedRow = rulesTable.selectedRow
            if (selectedRow >= 0) {
                val rule = rulesTableModel.getRuleAt(selectedRow)
                val updatedRule = rule.copy(enabled = !rule.enabled)
                mockServerManager.addRule(updatedRule) // Update by ID
                loadRules()
            }
        }
    }

    private fun updateServerStatus(running: Boolean, port: Int?) {
        SwingUtilities.invokeLater {
            if (running && port != null) {
                statusLabel.text = "Server: Running on port $port"
                startButton.isEnabled = false
                stopButton.isEnabled = true
                portField.isEnabled = false
            } else {
                statusLabel.text = "Server: Stopped"
                startButton.isEnabled = true
                stopButton.isEnabled = false
                portField.isEnabled = true
            }
        }
    }

    private fun updateButtonStates() {
        val hasSelection = rulesTable.selectedRow >= 0
        editRuleButton.isEnabled = hasSelection
        deleteRuleButton.isEnabled = hasSelection
        toggleRuleButton.isEnabled = hasSelection
    }

    private fun loadRules() {
        rulesTableModel.setRules(mockServerManager.allRules)
        updateButtonStates()
    }

    private fun showRuleDialog(existingRule: MockRule?) {
        val dialog = MockRuleDialog(project, existingRule)
        if (dialog.showAndGet()) {
            val rule = dialog.getMockRule()
            mockServerManager.addRule(rule)
            loadRules()
        }
    }

    /**
     * Table model for Mock Rules.
     */
    private class MockRulesTableModel : AbstractTableModel() {
        private val rules = mutableListOf<MockRule>()
        private val columns = arrayOf("Enabled", "Path", "Method", "Status", "Body Match", "Response Preview", "Delay(ms)")

        fun setRules(newRules: List<MockRule>) {
            rules.clear()
            rules.addAll(newRules)
            fireTableDataChanged()
        }

        fun getRuleAt(row: Int): MockRule = rules[row]

        override fun getRowCount(): Int = rules.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val rule = rules[rowIndex]
            return when (columnIndex) {
                0 -> if (rule.enabled) "✓" else "✗"
                1 -> rule.path
                2 -> rule.method.name
                3 -> rule.statusCode
                4 -> if (rule.bodyMatcher.mode != com.github.dhzhu.amateurpostman.models.BodyMatchMode.NONE) {
                    "${rule.bodyMatcher.mode.name.take(1)}: ${rule.bodyMatcher.pattern.take(20)}"
                } else "-"
                5 -> rule.body.take(50) + if (rule.body.length > 50) "..." else ""
                6 -> rule.delayMs
                else -> ""
            }
        }
    }
}
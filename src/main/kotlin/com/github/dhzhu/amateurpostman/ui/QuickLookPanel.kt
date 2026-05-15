package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.*
import com.github.dhzhu.amateurpostman.services.EnvironmentService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * A popup panel that displays variable resolution information with source highlighting.
 *
 * @property project The IntelliJ project
 * @property environmentService The environment service to get variable information
 */
class QuickLookPanel(private val project: Project) {
    private val environmentService = project.service<EnvironmentService>()

    /**
     * Shows the Quick Look popup near the caller component
     *
     * @param caller The component requesting the popup (usually the button that was clicked)
     * @param collectionId Optional collection ID for variable resolution
     */
    fun show(caller: Component, collectionId: String? = null) {
        val panel = createQuickLookPanel(collectionId)

        val popup: JBPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel)
            .setTitle("Environment Quick Look")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()

        popup.showUnderneathOf(caller)
    }

    private fun createQuickLookPanel(collectionId: String? = null): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        // Get variable resolution result
        val resolutionResult = environmentService.getAllVariablesWithSource(collectionId)

        // Create main content
        val contentPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(5)

        // Add sections for each variable scope
        val globalSection = createVariableSection("Global Variables", resolutionResult.globalVariables)
        contentPanel.add(globalSection, gbc)
        gbc.gridy++

        val collectionSection = createVariableSection("Collection Variables", resolutionResult.collectionVariables)
        contentPanel.add(collectionSection, gbc)
        gbc.gridy++

        val environmentSection = createVariableSection("Environment Variables", resolutionResult.environmentVariables)
        contentPanel.add(environmentSection, gbc)
        gbc.gridy++

        if (resolutionResult.temporaryVariables.isNotEmpty()) {
            val temporarySection = createVariableSection("Pre-request Script Variables", resolutionResult.temporaryVariables)
            contentPanel.add(temporarySection, gbc)
        }

        val scrollPane = JBScrollPane(contentPanel)
        scrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createVariableSection(title: String, variables: List<VariableWithSource>): JPanel {
        val sectionPanel = JBPanel<JBPanel<*>>(BorderLayout())
        sectionPanel.border = BorderFactory.createTitledBorder(title)

        if (variables.isEmpty()) {
            val emptyLabel = JBLabel("(No variables)")
            emptyLabel.border = JBUI.Borders.empty(5)
            sectionPanel.add(emptyLabel, BorderLayout.CENTER)
            return sectionPanel
        }

        // Create table for variables
        val tableModel = VariableWithSourceTableModel(variables)
        val table = JBTable(tableModel)

        // Set custom renderer to handle shadowed variables with strikethrough and high priority variables
        table.setDefaultRenderer(String::class.java, VariableWithSourceCellRenderer())

        // Configure column widths
        table.columnModel.getColumn(0).preferredWidth = 150 // Key
        table.columnModel.getColumn(1).preferredWidth = 250 // Final Value
        table.columnModel.getColumn(2).preferredWidth = 100 // Scope
        table.columnModel.getColumn(3).preferredWidth = 150 // Source

        // Add mouse listener to enable copy functionality on double-click
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) { // Double-click to copy value
                    val row = table.selectedRow
                    val col = table.selectedColumn

                    if (row >= 0) {
                        val variable = (table.model as VariableWithSourceTableModel).variables[row]

                        // Copy the final value to clipboard
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        val contents = StringSelection(variable.finalValue)
                        clipboard.setContents(contents, null)
                    }
                }
            }
        })

        val scrollPane = JBScrollPane(table)
        scrollPane.maximumSize = java.awt.Dimension(700, 150)

        sectionPanel.add(scrollPane, BorderLayout.CENTER)

        return sectionPanel
    }

    /**
     * Table model for VariableWithSource objects
     */
    private class VariableWithSourceTableModel(private val vars: List<VariableWithSource>) : AbstractTableModel() {
        val variables: List<VariableWithSource> get() = vars

        override fun getRowCount(): Int = vars.size

        override fun getColumnCount(): Int = 4

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Key"
                1 -> "Final Value"
                2 -> "Scope"
                3 -> "Source"
                else -> ""
            }
        }

        override fun getColumnClass(column: Int): Class<*> {
            return String::class.java
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val variable = vars[rowIndex]
            return when (columnIndex) {
                0 -> variable.key
                1 -> variable.finalValue
                2 -> variable.scope.name
                3 -> variable.sourceName
                else -> null
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    /**
     * Cell renderer to highlight shadowed variables with strikethrough and priority indicators
     */
    private class VariableWithSourceCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            // Find the corresponding VariableWithSource to check if it's shadowed
            val model = table.model as? VariableWithSourceTableModel
            if (model != null && row < model.variables.size) {
                val variable = model.variables[row]

                // Highlight based on whether the variable is shadowed or has high priority
                if (isSelected) {
                    return component
                }

                when {
                    // If this variable is shadowed by a higher priority one, apply strikethrough
                    variable.isShadowed -> {
                        component.foreground = Color.GRAY
                        component.font = component.font.deriveFont(component.font.style or Font.ITALIC)
                    }
                    // If this variable has high priority (not shadowed), highlight it
                    else -> {
                        when (variable.scope) {
                            VariableScope.ENVIRONMENT -> component.foreground = Color(0, 128, 0) // Green for environment
                            VariableScope.COLLECTION -> component.foreground = Color(0, 0, 139) // Dark blue for collection
                            VariableScope.GLOBAL -> component.foreground = Color(160, 160, 160) // Gray for global
                            VariableScope.TEMPORARY -> component.foreground = Color(255, 140, 0) // Orange for temporary
                        }
                        component.font = component.font.deriveFont(Font.BOLD)
                    }
                }
            }

            return component
        }
    }
}
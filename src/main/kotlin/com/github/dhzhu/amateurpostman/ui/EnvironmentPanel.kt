package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.Environment
import com.github.dhzhu.amateurpostman.models.Variable
import com.github.dhzhu.amateurpostman.services.EnvironmentChangeListener
import com.github.dhzhu.amateurpostman.services.EnvironmentService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

/**
 * Panel for managing environments and variables.
 *
 * Features:
 * - Environment selector combo box
 * - Variables table with Key, Value, Description, Enabled columns
 * - Add/Remove variable buttons
 * - Manage environments (create/delete/rename)
 * - Separate tab for global variables
 */
class EnvironmentPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val environmentService = project.service<EnvironmentService>()
    private val environments = mutableListOf<Environment>()

    // UI Components
    private val tabbedPane = JBTabbedPane()
    private val environmentPanel = JPanel(BorderLayout())
    private val globalPanel = JPanel(BorderLayout())

    // Environment tab components
    private val environmentComboBox = JComboBox<EnvironmentWrapper>()
    private val variablesTableModel = VariablesTableModel()
    private val variablesTable = JBTable(variablesTableModel)
    private val addVariableButton = JButton("Add Variable")
    private val removeVariableButton = JButton("Remove")
    private val manageEnvironmentsButton = JButton("Manage Environments")

    // Global variables tab components
    private val globalVariablesTableModel = VariablesTableModel()
    private val globalVariablesTable = JBTable(globalVariablesTableModel)
    private val addGlobalVariableButton = JButton("Add Variable")
    private val removeGlobalVariableButton = JButton("Remove")

    init {
        createUI()
        loadEnvironments()

        // Listen for environment changes
        environmentService.addChangeListener(object : EnvironmentChangeListener {
            override fun onEnvironmentChanged() {
                SwingUtilities.invokeLater {
                    loadEnvironments()
                }
            }
        })
    }

    private fun createUI() {
        border = JBUI.Borders.empty(5)

        // Create environment tab
        createEnvironmentTab()

        // Create global variables tab
        createGlobalVariablesTab()

        // Add tabs
        tabbedPane.addTab("Environments", environmentPanel)
        tabbedPane.addTab("Global Variables", globalPanel)

        add(tabbedPane, BorderLayout.CENTER)
    }

    private fun createEnvironmentTab() {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // Environment selector row
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5)
        mainPanel.add(JBLabel("Environment:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        mainPanel.add(environmentComboBox, gbc)

        gbc.gridx = 2
        gbc.weightx = 0.0
        mainPanel.add(manageEnvironmentsButton, gbc)

        // Variables table
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 3
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = JBUI.insets(5)

        val tableScrollPane = JBScrollPane(variablesTable)
        tableScrollPane.border = BorderFactory.createTitledBorder("Variables")
        mainPanel.add(tableScrollPane, gbc)

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(addVariableButton)
        buttonPanel.add(removeVariableButton)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 3
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(5)
        mainPanel.add(buttonPanel, gbc)

        // Setup table
        setupVariablesTable(variablesTable)

        // Setup listeners
        setupEnvironmentListeners()

        environmentPanel.add(mainPanel, BorderLayout.CENTER)
    }

    private fun createGlobalVariablesTab() {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // Title
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(5)
        mainPanel.add(
            JBLabel("<html><b>Global Variables</b> - Available in all environments</html></body>"),
            gbc
        )

        // Variables table
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = JBUI.insets(5)

        val tableScrollPane = JBScrollPane(globalVariablesTable)
        tableScrollPane.border = BorderFactory.createTitledBorder("Variables")
        mainPanel.add(tableScrollPane, gbc)

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        buttonPanel.add(addGlobalVariableButton)
        buttonPanel.add(removeGlobalVariableButton)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(5)
        mainPanel.add(buttonPanel, gbc)

        // Setup table
        setupVariablesTable(globalVariablesTable)

        // Setup listeners
        setupGlobalVariableListeners()

        globalPanel.add(mainPanel, BorderLayout.CENTER)
    }

    private fun setupVariablesTable(table: JTable) {
        table.columnModel.getColumn(0).preferredWidth = 150 // Key
        table.columnModel.getColumn(1).preferredWidth = 200 // Value
        table.columnModel.getColumn(2).preferredWidth = 200 // Description
        table.columnModel.getColumn(3).preferredWidth = 50  // Enabled
        table.rowHeight = 25
        table.autoCreateRowSorter = true
    }

    private fun setupEnvironmentListeners() {
        // Environment selection changed
        environmentComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val selected = e.item as? EnvironmentWrapper
                selected?.let { wrapper ->
                    wrapper.environment?.let { env ->
                        environmentService.setCurrentEnvironment(env.id)
                        loadVariables()
                    }
                }
            }
        }

        // Add variable
        addVariableButton.addActionListener { addVariable() }

        // Remove variable
        removeVariableButton.addActionListener { removeSelectedVariable() }

        // Manage environments
        manageEnvironmentsButton.addActionListener { showManageEnvironmentsDialog() }
    }

    private fun setupGlobalVariableListeners() {
        // Add global variable
        addGlobalVariableButton.addActionListener { addGlobalVariable() }

        // Remove global variable
        removeGlobalVariableButton.addActionListener { removeSelectedGlobalVariable() }
    }

    private fun loadEnvironments() {
        // Load all environments
        environments.clear()
        environments.addAll(environmentService.getEnvironments())

        // Update combo box
        environmentComboBox.removeAllItems()

        // Add "No Environment" option
        environmentComboBox.addItem(EnvironmentWrapper(null))

        // Add all environments
        environments.forEach { env ->
            environmentComboBox.addItem(EnvironmentWrapper(env))
        }

        // Set current selection
        val current = environmentService.getCurrentEnvironment()
        if (current != null) {
            val wrapper = EnvironmentWrapper(current)
            environmentComboBox.selectedItem = wrapper
        } else {
            environmentComboBox.selectedIndex = 0
        }

        // Load variables for current environment
        loadVariables()
        loadGlobalVariables()
    }

    private fun loadVariables() {
        val current = environmentService.getCurrentEnvironment()

        if (current != null) {
            variablesTableModel.setVariables(current.variables)
        } else {
            variablesTableModel.setVariables(emptyList())
        }

        variablesTableModel.fireTableDataChanged()
    }

    private fun loadGlobalVariables() {
        val globals = environmentService.getGlobalVariables()
        globalVariablesTableModel.setVariables(globals)
        globalVariablesTableModel.fireTableDataChanged()
    }

    private fun addVariable() {
        val current = environmentService.getCurrentEnvironment()
        if (current == null) {
            Messages.showWarningDialog(
                project,
                "Please select or create an environment first.",
                "No Environment Selected"
            )
            return
        }

        val newVariable = Variable("new_variable", "value", "Description", enabled = true)
        environmentService.addVariable(current.id, newVariable)
        loadVariables()
    }

    private fun removeSelectedVariable() {
        val selectedRow = variablesTable.selectedRow
        if (selectedRow < 0) {
            return
        }

        val current = environmentService.getCurrentEnvironment() ?: return
        val key = variablesTableModel.getKeyAt(selectedRow)

        environmentService.removeVariable(current.id, key)
        loadVariables()
    }

    private fun addGlobalVariable() {
        val newVariable = Variable("new_global", "value", "Description", enabled = true)
        environmentService.setGlobalVariable(newVariable)
        loadGlobalVariables()
    }

    private fun removeSelectedGlobalVariable() {
        val selectedRow = globalVariablesTable.selectedRow
        if (selectedRow < 0) {
            return
        }

        val key = globalVariablesTableModel.getKeyAt(selectedRow)
        environmentService.removeGlobalVariable(key)
        loadGlobalVariables()
    }

    private fun showManageEnvironmentsDialog() {
        val options = arrayOf("Create New Environment", "Rename Current", "Delete Current", "Cancel")
        val current = environmentService.getCurrentEnvironment()

        val title = if (current != null) {
            "Manage Environments (Current: ${current.name})"
        } else {
            "Manage Environments"
        }

        val choice = Messages.showDialog(
            project,
            "What would you like to do?",
            title,
            options,
            3, // Default to Cancel
            Messages.getQuestionIcon()
        )

        when (choice) {
            0 -> createNewEnvironment()
            1 -> renameCurrentEnvironment()
            2 -> deleteCurrentEnvironment()
        }
    }

    private fun createNewEnvironment() {
        val name = Messages.showInputDialog(
            project,
            "Enter environment name:",
            "Create New Environment",
            null
        ) ?: return

        if (name.isBlank()) {
            Messages.showErrorDialog(project, "Environment name cannot be empty.", "Invalid Name")
            return
        }

        val newEnv = environmentService.createEnvironment(name)
        environmentService.setCurrentEnvironment(newEnv.id)
        loadEnvironments()
    }

    private fun renameCurrentEnvironment() {
        val current = environmentService.getCurrentEnvironment()
        if (current == null) {
            Messages.showWarningDialog(project, "No environment selected.", "Cannot Rename")
            return
        }

        val newName = Messages.showInputDialog(
            project,
            "Enter new name for '${current.name}':",
            "Rename Environment",
            null
        ) ?: return

        if (newName.isBlank()) {
            Messages.showErrorDialog(project, "Environment name cannot be empty.", "Invalid Name")
            return
        }

        environmentService.renameEnvironment(current.id, newName)
        loadEnvironments()
    }

    private fun deleteCurrentEnvironment() {
        val current = environmentService.getCurrentEnvironment()
        if (current == null) {
            Messages.showWarningDialog(project, "No environment selected.", "Cannot Delete")
            return
        }

        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete environment '${current.name}'?",
            "Delete Environment",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            environmentService.deleteEnvironment(current.id)
            loadEnvironments()
        }
    }

    /**
     * Wrapper class for displaying environments in combo box.
     */
    private class EnvironmentWrapper(val environment: Environment?) {
        override fun toString(): String {
            return environment?.name ?: "No Environment"
        }
    }

    /**
     * Table model for variables.
     */
    private class VariablesTableModel : DefaultTableModel() {
        private val keys = mutableListOf<String>()
        private var variables: List<Variable> = emptyList()

        fun setVariables(vars: List<Variable>) {
            variables = vars
            keys.clear()
            keys.addAll(vars.map { it.key })
        }

        fun getKeyAt(row: Int): String {
            return keys.getOrNull(row) ?: ""
        }

        override fun getRowCount(): Int = variables.size

        override fun getColumnCount(): Int = 4

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Key"
                1 -> "Value"
                2 -> "Description"
                3 -> "Enabled"
                else -> ""
            }
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                3 -> Boolean::class.javaObjectType
                else -> String::class.java
            }
        }

        override fun getValueAt(row: Int, column: Int): Any {
            val variable = variables.getOrNull(row) ?: return ""
            return when (column) {
                0 -> variable.key
                1 -> variable.value
                2 -> variable.description
                3 -> variable.enabled
                else -> ""
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean = true

        override fun setValueAt(value: Any?, row: Int, column: Int) {
            val variable = variables.getOrNull(row) ?: return
            val updatedVariable = when (column) {
                0 -> variable.copy(key = value as String)
                1 -> variable.copy(value = value as String)
                2 -> variable.copy(description = value as String)
                3 -> variable.copy(enabled = value as Boolean)
                else -> variable
            }
            variables = variables.toMutableList().apply { set(row, updatedVariable) }
            keys[row] = updatedVariable.key
            fireTableCellUpdated(row, column)
        }
    }
}

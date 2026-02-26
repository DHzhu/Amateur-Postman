package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.GraphQLRequest
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane

/**
 * Panel for editing GraphQL requests.
 * Provides separate editors for Query, Variables, and Operation Name.
 */
class GraphQLPanel {

    private lateinit var queryArea: JBTextArea
    private lateinit var variablesArea: JBTextArea
    private lateinit var operationNameField: JBTextField
    private lateinit var prettyPrintButton: JButton

    /**
     * Creates the GraphQL editor panel
     */
    fun createPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())

        // Toolbar with operation name and pretty print button
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
        toolbar.add(JLabel("Operation Name (optional):"))
        operationNameField = JBTextField(20)
        toolbar.add(operationNameField)

        prettyPrintButton = JButton("Prettify")
        prettyPrintButton.addActionListener { prettifyQuery() }
        toolbar.add(prettyPrintButton)

        mainPanel.add(toolbar, BorderLayout.NORTH)

        // Split pane: Query (top) and Variables (bottom)
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)

        // Query panel
        val queryPanel = JPanel(BorderLayout())
        queryPanel.border = javax.swing.BorderFactory.createTitledBorder("Query")
        queryArea = JBTextArea()
        queryArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        queryArea.tabSize = 2
        queryPanel.add(JBScrollPane(queryArea), BorderLayout.CENTER)
        splitPane.topComponent = queryPanel

        // Variables panel
        val variablesPanel = JPanel(BorderLayout())
        variablesPanel.border = javax.swing.BorderFactory.createTitledBorder("Variables (JSON)")
        variablesArea = JBTextArea()
        variablesArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        variablesArea.tabSize = 2
        variablesPanel.add(JBScrollPane(variablesArea), BorderLayout.CENTER)
        splitPane.bottomComponent = variablesPanel

        splitPane.resizeWeight = 0.7 // Give more space to query
        mainPanel.add(splitPane, BorderLayout.CENTER)

        return mainPanel
    }

    /**
     * Returns the current GraphQL request from the panel
     */
    fun getGraphQLRequest(): GraphQLRequest {
        return GraphQLRequest(
            query = queryArea.text,
            operationName = operationNameField.text.takeIf { it.isNotBlank() },
            variables = variablesArea.text.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Loads a GraphQL request into the panel
     */
    fun loadGraphQLRequest(request: GraphQLRequest) {
        queryArea.text = request.query
        operationNameField.text = request.operationName ?: ""
        variablesArea.text = request.variables ?: ""
    }

    /**
     * Returns the JSON representation of the GraphQL request
     */
    fun toJson(): String {
        return getGraphQLRequest().toJson()
    }

    /**
     * Clears all fields in the panel
     */
    fun clear() {
        queryArea.text = ""
        operationNameField.text = ""
        variablesArea.text = ""
    }

    /**
     * Checks if the panel has any content
     */
    fun isEmpty(): Boolean {
        return queryArea.text.isBlank()
    }

    /**
     * Attempts to prettify/format the GraphQL query
     * Public method to allow external invocation
     */
    fun prettifyQuery() {
        val query = queryArea.text
        if (query.isBlank()) return

        try {
            val formatted = formatGraphQL(query)
            queryArea.text = formatted
        } catch (e: Exception) {
            // Silently fail if formatting fails
        }
    }

    /**
     * Basic GraphQL query formatting
     */
    private fun formatGraphQL(query: String): String {
        val trimmed = query.trim()
        val formatted = StringBuilder()
        var indentLevel = 0
        val indent = "  "

        var i = 0
        while (i < trimmed.length) {
            val c = trimmed[i]

            when (c) {
                '{' -> {
                    formatted.append(c).append('\n').append(indent.repeat(++indentLevel))
                }
                '}' -> {
                    formatted.append('\n').append(indent.repeat(--indentLevel)).append(c)
                }
                '(' -> {
                    formatted.append(c)
                }
                ')' -> {
                    formatted.append(c)
                }
                ',' -> {
                    formatted.append(c).append('\n').append(indent.repeat(indentLevel))
                }
                ':' -> {
                    formatted.append(c).append(' ')
                }
                '\n', '\r' -> {
                    // Skip existing newlines
                }
                ' ' -> {
                    // Skip extra spaces
                    if (i > 0 && trimmed[i - 1] != ' ' && trimmed[i - 1] != '\n') {
                        formatted.append(c)
                    }
                }
                else -> {
                    formatted.append(c)
                }
            }
            i++
        }

        return formatted.toString()
    }

    /**
     * Sets the query text directly
     */
    fun setQuery(query: String) {
        queryArea.text = query
    }

    /**
     * Sets the variables JSON text directly
     */
    fun setVariables(variables: String) {
        variablesArea.text = variables
    }

    /**
     * Gets the query text
     */
    fun getQuery(): String = queryArea.text

    /**
     * Gets the variables text
     */
    fun getVariables(): String = variablesArea.text

    /**
     * Gets the operation name
     */
    fun getOperationName(): String = operationNameField.text
}

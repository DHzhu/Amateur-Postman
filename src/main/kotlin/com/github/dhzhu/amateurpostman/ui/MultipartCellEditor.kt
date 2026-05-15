package com.github.dhzhu.amateurpostman.ui

import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.table.TableCellEditor

/**
 * Custom cell editor for Multipart table Value column.
 * Shows a text field with a browse button for file selection.
 */
class MultipartCellEditor(
    private val panel: PostmanToolWindowPanel
) : AbstractCellEditor(), TableCellEditor {

    private val editorPanel = JPanel(java.awt.BorderLayout(5, 0))
    private val textField = javax.swing.JTextField()
    private val browseButton = JButton("...")

    init {
        editorPanel.add(textField, java.awt.BorderLayout.CENTER)
        editorPanel.add(browseButton, java.awt.BorderLayout.EAST)

        browseButton.addActionListener {
            val filePath = panel.browseForFile()
            if (filePath != null) {
                textField.text = filePath
                // Update the table model
                stopCellEditing()
            }
        }

        // Handle Enter key to commit editing
        textField.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "commit")
        textField.actionMap.put("commit", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                stopCellEditing()
            }
        })
    }

    override fun getTableCellEditorComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        textField.text = value?.toString() ?: ""

        // Check the Type column to determine if this is a file field
        val typeValue = table?.getModel()?.getValueAt(row, 1)?.toString()
        if (typeValue == "File") {
            browseButton.isEnabled = true
        } else {
            browseButton.isEnabled = false
        }

        return editorPanel
    }

    override fun getCellEditorValue(): Any {
        return textField.text
    }
}

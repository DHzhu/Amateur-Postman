package com.github.dhzhu.amateurpostman.ui

import com.intellij.ui.table.JBTable
import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import javax.swing.AbstractCellEditor
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

/**
 * Reusable helper that adds inline per-row action buttons to a [JBTable].
 *
 * Each data row gets a delete (−) button in the last column.
 * The trailing empty row shows an add (+) button instead.
 * The helper auto-appends a trailing empty row so users always have a blank row ready.
 */
object InlineTableActionsHelper {

    /** Column header for the actions column. */
    const val ACTIONS_COLUMN_HEADER = ""

    /**
     * Adds an actions column to the given [table] and wires up inline actions.
     *
     * @param table      The JBTable to augment.
     * @param model      The table's DefaultTableModel.
     * @param dataColumnCount Number of data columns (excluding the actions column).
     */
    fun addActionsColumn(table: JBTable, model: DefaultTableModel, dataColumnCount: Int) {
        model.addColumn(ACTIONS_COLUMN_HEADER)

        val actionsColIndex = dataColumnCount

        table.columnModel.getColumn(actionsColIndex).apply {
            preferredWidth = 32
            maxWidth = 40
            minWidth = 28
            cellRenderer = ActionsCellRenderer()
            cellEditor = ActionsCellEditor(table, model)
        }
    }

    /**
     * Adds a new empty row at the end of the model and scrolls to it.
     */
    fun addEmptyRow(table: JBTable, model: DefaultTableModel, dataColumnCount: Int) {
        val row = Array(dataColumnCount + 1) { "" }
        model.addRow(row)
        val newRow = model.rowCount - 1
        table.setRowSelectionInterval(newRow, newRow)
        table.scrollRectToVisible(table.getCellRect(newRow, 0, true))
    }

    /**
     * Ensures there is always a trailing empty row ready for input.
     * Call this after any row deletion or data load.
     */
    fun ensureTrailingEmptyRow(table: JBTable, model: DefaultTableModel, dataColumnCount: Int) {
        if (model.rowCount == 0) {
            addEmptyRow(table, model, dataColumnCount)
            return
        }
        val lastRow = model.rowCount - 1
        val isEmpty = (0 until dataColumnCount).all { col ->
            val value = model.getValueAt(lastRow, col)?.toString()?.trim()
            value.isNullOrEmpty()
        }
        if (!isEmpty) {
            addEmptyRow(table, model, dataColumnCount)
        }
    }

    private fun isTrailingEmptyRow(model: DefaultTableModel, row: Int): Boolean {
        val dataColCount = model.columnCount - 1
        val isLastRow = row == model.rowCount - 1
        val isDataEmpty = (0 until dataColCount).all { col ->
            model.getValueAt(row, col)?.toString()?.trim().isNullOrEmpty()
        }
        return isLastRow && isDataEmpty
    }

    // ─── Cell Renderer ────────────────────────────────────────────────────────

    private class ActionsCellRenderer : JLabel(), TableCellRenderer {
        init {
            horizontalAlignment = CENTER
            font = Font("Dialog", Font.PLAIN, 14)
            foreground = JBColor(0x969696, 0x8A8A8A)
            preferredSize = Dimension(28, 20)
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val model = table?.model as? DefaultTableModel
            text = if (model != null && isTrailingEmptyRow(model, row)) "+" else "−"
            background = if (isSelected) table?.selectionBackground else table?.background
            isOpaque = false
            return this
        }
    }

    // ─── Cell Editor ──────────────────────────────────────────────────────────

    private class ActionsCellEditor(
        private val table: JTable,
        private val model: DefaultTableModel
    ) : AbstractCellEditor(), TableCellEditor {

        private val button = JButton().apply {
            font = Font("Dialog", Font.PLAIN, 14)
            foreground = JBColor(0x969696, 0x8A8A8A)
            border = BorderFactory.createEmptyBorder()
            isContentAreaFilled = false
            isBorderPainted = false
            preferredSize = Dimension(28, 20)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        init {
            button.addActionListener {
                val row = table.editingRow
                if (row < 0 || row >= model.rowCount) {
                    fireEditingStopped()
                    return@addActionListener
                }

                val jbTable = table as JBTable
                val dataColCount = model.columnCount - 1

                if (isTrailingEmptyRow(model, row)) {
                    // "+" clicked — add new row
                    addEmptyRow(jbTable, model, dataColCount)
                } else {
                    // "−" clicked — delete row (but not if it's the only empty row)
                    if (!(model.rowCount == 1)) {
                        model.removeRow(row)
                        ensureTrailingEmptyRow(jbTable, model, dataColCount)
                        if (model.rowCount > 0) {
                            val newSelection = if (row > 0) row - 1 else 0
                            table.setRowSelectionInterval(newSelection, newSelection)
                        }
                    }
                }
                fireEditingStopped()
            }
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            button.text = if (isTrailingEmptyRow(model, row)) "+" else "−"
            return button
        }

        override fun getCellEditorValue(): Any = ""
    }
}

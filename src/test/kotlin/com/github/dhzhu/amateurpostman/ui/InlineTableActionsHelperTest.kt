package com.github.dhzhu.amateurpostman.ui

import com.intellij.ui.table.JBTable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.table.DefaultTableModel

class InlineTableActionsHelperTest {

    private lateinit var model: DefaultTableModel
    private lateinit var table: JBTable
    private val dataColumnCount = 3

    @BeforeEach
    fun setup() {
        model = DefaultTableModel(arrayOf("Key", "Value", "Description"), 0)
        table = JBTable(model)
        InlineTableActionsHelper.addActionsColumn(table, model, dataColumnCount)
    }

    @Test
    fun `addActionsColumn adds one extra column`() {
        assertEquals(dataColumnCount + 1, model.columnCount)
        assertEquals(InlineTableActionsHelper.ACTIONS_COLUMN_HEADER, model.getColumnName(dataColumnCount))
    }

    @Test
    fun `actions column has narrow width`() {
        val col = table.columnModel.getColumn(dataColumnCount)
        assertTrue(col.preferredWidth <= 40)
        assertTrue(col.maxWidth <= 40)
    }

    @Test
    fun `addEmptyRow adds a row with correct column count`() {
        InlineTableActionsHelper.addEmptyRow(table, model, dataColumnCount)
        assertEquals(1, model.rowCount)
        assertEquals(dataColumnCount + 1, model.columnCount)
        // Data columns should be empty
        assertEquals("", model.getValueAt(0, 0))
        assertEquals("", model.getValueAt(0, 1))
        assertEquals("", model.getValueAt(0, 2))
    }

    @Test
    fun `addEmptyRow scrolls to new row`() {
        javax.swing.SwingUtilities.invokeAndWait {
            InlineTableActionsHelper.addEmptyRow(table, model, dataColumnCount)
        }
        assertEquals(1, model.rowCount)
    }

    @Test
    fun `ensureTrailingEmptyRow adds row when model is empty`() {
        assertEquals(0, model.rowCount)
        InlineTableActionsHelper.ensureTrailingEmptyRow(table, model, dataColumnCount)
        assertEquals(1, model.rowCount)
    }

    @Test
    fun `ensureTrailingEmptyRow adds row when last row has data`() {
        // Add a row with data
        model.addRow(arrayOf("key1", "val1", "desc1", ""))
        assertEquals(1, model.rowCount)

        InlineTableActionsHelper.ensureTrailingEmptyRow(table, model, dataColumnCount)
        assertEquals(2, model.rowCount)
        // Last row should be empty
        assertEquals("", model.getValueAt(1, 0))
    }

    @Test
    fun `ensureTrailingEmptyRow does not duplicate when last row is already empty`() {
        // Add an empty row
        model.addRow(arrayOf("", "", "", ""))
        assertEquals(1, model.rowCount)

        InlineTableActionsHelper.ensureTrailingEmptyRow(table, model, dataColumnCount)
        assertEquals(1, model.rowCount) // Should NOT add another
    }

    @Test
    fun `multiple addEmptyRow calls increase row count`() {
        InlineTableActionsHelper.addEmptyRow(table, model, dataColumnCount)
        InlineTableActionsHelper.addEmptyRow(table, model, dataColumnCount)
        InlineTableActionsHelper.addEmptyRow(table, model, dataColumnCount)
        assertEquals(3, model.rowCount)
    }

}

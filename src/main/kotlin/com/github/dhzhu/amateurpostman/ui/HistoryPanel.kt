package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.RequestHistoryEntry
import com.github.dhzhu.amateurpostman.services.RequestHistoryService
import com.github.dhzhu.amateurpostman.utils.CurlExporter
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/** Panel for displaying and managing request history */
class HistoryPanel(
        private val project: Project,
        private val onLoadRequest: (RequestHistoryEntry) -> Unit
) : JPanel(BorderLayout()) {

    private val historyService = project.service<RequestHistoryService>()
    private val listModel = DefaultListModel<RequestHistoryEntry>()
    private val historyList = JBList(listModel)
    private val searchField = JBTextField()
    private val countLabel = JLabel("0 items")

    init {
        createUI()
        loadHistory()

        // Listen for history changes
        historyService.addChangeListener { SwingUtilities.invokeLater { loadHistory() } }
    }

    private fun createUI() {
        border = JBUI.Borders.empty(5)

        // Search panel
        val searchPanel = JPanel(BorderLayout(5, 0))
        searchPanel.border = JBUI.Borders.emptyBottom(5)

        searchField.emptyText.text = "Search history..."
        searchField.addKeyListener(
                object : KeyAdapter() {
                    override fun keyReleased(e: KeyEvent) {
                        filterHistory()
                    }
                }
        )

        searchPanel.add(searchField, BorderLayout.CENTER)
        searchPanel.add(countLabel, BorderLayout.EAST)

        // History list
        historyList.cellRenderer = HistoryListCellRenderer()
        historyList.addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.clickCount == 2) {
                            loadSelectedEntry()
                        }
                    }

                    override fun mousePressed(e: MouseEvent) {
                        if (e.isPopupTrigger) {
                            showContextMenu(e)
                        }
                    }

                    override fun mouseReleased(e: MouseEvent) {
                        if (e.isPopupTrigger) {
                            showContextMenu(e)
                        }
                    }
                }
        )

        historyList.addKeyListener(
                object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (e.keyCode == KeyEvent.VK_ENTER) {
                            loadSelectedEntry()
                        } else if (e.keyCode == KeyEvent.VK_DELETE) {
                            deleteSelectedEntry()
                        }
                    }
                }
        )

        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        buttonPanel.border = JBUI.Borders.emptyTop(5)

        val loadButton = JButton("Load")
        loadButton.addActionListener { loadSelectedEntry() }

        val deleteButton = JButton("Delete")
        deleteButton.addActionListener { deleteSelectedEntry() }

        val clearButton = JButton("Clear All")
        clearButton.addActionListener { clearHistory() }

        buttonPanel.add(loadButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(clearButton)

        // Layout
        add(searchPanel, BorderLayout.NORTH)
        add(JBScrollPane(historyList), BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun loadHistory() {
        val query = searchField.text
        val entries =
                if (query.isNullOrBlank()) {
                    historyService.getHistory()
                } else {
                    historyService.searchHistory(query)
                }

        listModel.clear()
        entries.forEach { listModel.addElement(it) }
        countLabel.text = "${entries.size} items"
    }

    private fun filterHistory() {
        loadHistory()
    }

    private fun loadSelectedEntry() {
        val selected = historyList.selectedValue
        if (selected != null) {
            onLoadRequest(selected)
        }
    }

    private fun deleteSelectedEntry() {
        val selected = historyList.selectedValue
        if (selected != null) {
            historyService.deleteEntry(selected.id)
        }
    }

    private fun clearHistory() {
        val result =
                Messages.showYesNoDialog(
                        project,
                        "Are you sure you want to clear all history?",
                        "Clear History",
                        Messages.getQuestionIcon()
                )
        if (result == Messages.YES) {
            historyService.clearHistory()
        }
    }

    private fun showContextMenu(e: MouseEvent) {
        val index = historyList.locationToIndex(e.point)
        if (index >= 0) {
            historyList.selectedIndex = index
            val entry = listModel.getElementAt(index)

            val popup = JPopupMenu()

            val loadItem = JMenuItem("Load Request")
            loadItem.addActionListener { onLoadRequest(entry) }
            popup.add(loadItem)

            val copyUrlItem = JMenuItem("Copy URL")
            copyUrlItem.addActionListener { copyToClipboard(entry.request.url) }
            popup.add(copyUrlItem)

            val copyCurlItem = JMenuItem("Copy as cURL")
            copyCurlItem.addActionListener {
                val curl = CurlExporter.export(entry.request)
                copyToClipboard(curl)
            }
            popup.add(copyCurlItem)

            popup.addSeparator()

            val renameItem = JMenuItem("Rename...")
            renameItem.addActionListener {
                val newName =
                        Messages.showInputDialog(
                                project,
                                "Enter name:",
                                "Rename Entry",
                                null,
                                entry.name ?: entry.getDisplayName(),
                                null
                        )
                if (newName != null) {
                    historyService.renameEntry(entry.id, newName)
                }
            }
            popup.add(renameItem)

            val deleteItem = JMenuItem("Delete")
            deleteItem.addActionListener { historyService.deleteEntry(entry.id) }
            popup.add(deleteItem)

            popup.show(historyList, e.x, e.y)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    /** Refresh the history list */
    fun refresh() {
        loadHistory()
    }

    /** Custom cell renderer for history list */
    private class HistoryListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is RequestHistoryEntry) {
                val statusColor =
                        when {
                            value.response == null -> Color.GRAY
                            value.response.isSuccessful -> Color(0, 150, 0)
                            else -> Color(200, 0, 0)
                        }

                text = buildString {
                    append("<html>")
                    append("<b>${value.request.method}</b> ")
                    append(value.getDisplayName().replace(value.request.method.name + " ", ""))
                    append("<br/>")
                    append("<small><font color='gray'>${value.getFormattedTime()}</font>")
                    if (value.response != null) {
                        val colorHex =
                                String.format(
                                        "#%02x%02x%02x",
                                        statusColor.red,
                                        statusColor.green,
                                        statusColor.blue
                                )
                        append(" | <font color='$colorHex'>${value.getStatusIndicator()}</font>")
                        append(" | ${value.response.duration}ms")
                    }
                    append("</small>")
                    append("</html>")
                }

                border = JBUI.Borders.empty(3, 5)
            }

            return this
        }
    }
}

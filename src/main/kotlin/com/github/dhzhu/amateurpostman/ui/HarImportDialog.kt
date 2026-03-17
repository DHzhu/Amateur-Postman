package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.utils.HarConverter
import com.github.dhzhu.amateurpostman.utils.HarParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.tree.DefaultTreeModel

/**
 * Dialog for importing HTTP requests from a HAR (HTTP Archive) file.
 *
 * Workflow:
 * 1. User picks a .har file via the Browse button.
 * 2. Entries are parsed and shown as a checkbox tree grouped by host.
 * 3. Static resources (images, fonts, CSS…) are deselected by default.
 * 4. User adjusts selection and clicks Import.
 *
 * Returns [HarConverter.ImportResult] on success, null if cancelled.
 */
class HarImportDialog(private val project: Project) : DialogWrapper(project) {

    private val filePathField = JTextField(40).apply { isEditable = false }
    private val browseButton = JButton("Browse…")
    private val filterStaticCheckBox = JCheckBox("Filter static resources", true)

    private val rootNode = CheckedTreeNode("root")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = CheckboxTree(HarEntryRenderer(), rootNode).apply {
        model = treeModel
        isRootVisible = false
    }

    private var parsedLog: HarParser.HarLog? = null

    data class DialogResult(
        val collectionName: String,
        val selectedEntries: List<HarParser.HarEntry>
    )

    init {
        title = "Import HAR File"
        setOKButtonText("Import")
        isOKActionEnabled = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            fill = GridBagConstraints.HORIZONTAL
        }

        // Row 0 – file picker
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        panel.add(JBLabel("HAR File:"), gbc)

        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(filePathField, gbc)

        gbc.gridx = 2; gbc.weightx = 0.0
        panel.add(browseButton, gbc)

        // Row 1 – filter checkbox
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2
        panel.add(filterStaticCheckBox, gbc)

        // Row 2 – tree (hidden until a file is loaded)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0
        val scrollPane = JBScrollPane(tree).apply {
            preferredSize = java.awt.Dimension(600, 350)
        }
        panel.add(scrollPane, gbc)

        // Row 3 – select-all / deselect-all buttons
        gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0.0
        val selectionBar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            val selectAllBtn = JButton("Select All").apply {
                addActionListener { setAllChecked(true) }
            }
            val deselectAllBtn = JButton("Deselect All").apply {
                addActionListener { setAllChecked(false) }
            }
            add(selectAllBtn)
            add(Box.createHorizontalStrut(6))
            add(deselectAllBtn)
            add(Box.createHorizontalGlue())
        }
        panel.add(selectionBar, gbc)

        // Wire up Browse button
        browseButton.addActionListener { onBrowse() }

        // Re-populate tree when filter checkbox changes
        filterStaticCheckBox.addActionListener { populateTree() }

        return panel
    }

    override fun doValidate(): ValidationInfo? {
        if (parsedLog == null) return ValidationInfo("Please select a HAR file.", browseButton)
        if (collectSelectedEntries().isEmpty()) return ValidationInfo("Select at least one request to import.", tree)
        return null
    }

    /** Returns the result to be used by the caller after [showAndGet] == true. */
    fun getDialogResult(): DialogResult? {
        val entries = collectSelectedEntries()
        if (entries.isEmpty()) return null
        val name = filePathField.text
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .removeSuffix(".har")
            .ifBlank { "HAR Import" }
        return DialogResult(collectionName = name, selectedEntries = entries)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun onBrowse() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select HAR File"
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                "HAR Files (*.har)", "har"
            )
        }
        if (chooser.showOpenDialog(contentPanel) != JFileChooser.APPROVE_OPTION) return

        val file = chooser.selectedFile
        filePathField.text = file.absolutePath

        when (val result = HarParser.parseFromFile(file)) {
            is HarParser.HarParseResult.Success -> {
                parsedLog = result.log
                populateTree()
                isOKActionEnabled = true
            }
            is HarParser.HarParseResult.Failure -> {
                parsedLog = null
                rootNode.removeAllChildren()
                treeModel.reload()
                isOKActionEnabled = false
                com.intellij.openapi.ui.Messages.showErrorDialog(
                    project,
                    "Failed to parse HAR file:\n${result.message}",
                    "HAR Parse Error"
                )
            }
        }
    }

    private fun populateTree() {
        val log = parsedLog ?: return
        rootNode.removeAllChildren()

        val filterStatic = filterStaticCheckBox.isSelected
        val groups = HarConverter.groupByHost(log.entries, filterStatic)

        groups.forEach { (host, entries) ->
            val hostNode = CheckedTreeNode(host).apply { isChecked = true }
            entries.forEach { entry ->
                val path = try {
                    java.net.URI(entry.request.url).path?.takeIf { it.isNotBlank() } ?: "/"
                } catch (_: Exception) {
                    entry.request.url
                }
                val label = "${entry.request.method}  $path"
                val entryNode = HarEntryNode(label, entry).apply { isChecked = true }
                hostNode.add(entryNode)
            }
            if (hostNode.childCount > 0) rootNode.add(hostNode)
        }

        treeModel.reload()
        expandAllNodes()
    }

    private fun collectSelectedEntries(): List<HarParser.HarEntry> {
        val result = mutableListOf<HarParser.HarEntry>()
        forEachLeafNode { node ->
            if (node.isChecked) result.add(node.entry)
        }
        return result
    }

    private fun setAllChecked(checked: Boolean) {
        forEachLeafNode { it.isChecked = checked }
        treeModel.reload()
    }

    private fun forEachLeafNode(action: (HarEntryNode) -> Unit) {
        for (i in 0 until rootNode.childCount) {
            val hostNode = rootNode.getChildAt(i) as? CheckedTreeNode ?: continue
            for (j in 0 until hostNode.childCount) {
                (hostNode.getChildAt(j) as? HarEntryNode)?.let(action)
            }
        }
    }

    private fun expandAllNodes() {
        for (i in 0 until tree.rowCount) tree.expandRow(i)
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /** Tree node that carries a reference to the original [HarParser.HarEntry]. */
    private class HarEntryNode(label: String, val entry: HarParser.HarEntry) : CheckedTreeNode(label)

    /** Cell renderer for the checkbox tree. */
    private class HarEntryRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            val label = node.userObject?.toString() ?: ""
            if (leaf) {
                // Request entry — colour-code by method
                val method = label.substringBefore(" ").trim()
                val color = when (method) {
                    "GET"    -> java.awt.Color(0x2E7D32)
                    "POST"   -> java.awt.Color(0x1565C0)
                    "PUT"    -> java.awt.Color(0xE65100)
                    "DELETE" -> java.awt.Color(0xC62828)
                    "PATCH"  -> java.awt.Color(0x6A1B9A)
                    else     -> null
                }
                if (color != null) {
                    textRenderer.append(method, com.intellij.ui.SimpleTextAttributes(
                        com.intellij.ui.SimpleTextAttributes.STYLE_BOLD, color
                    ))
                    textRenderer.append(label.removePrefix(method), com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES)
                } else {
                    textRenderer.append(label)
                }
            } else {
                // Host folder node
                textRenderer.append(label, com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                val childCount = node.childCount
                textRenderer.append(
                    "  ($childCount)",
                    com.intellij.ui.SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES
                )
            }
        }
    }
}

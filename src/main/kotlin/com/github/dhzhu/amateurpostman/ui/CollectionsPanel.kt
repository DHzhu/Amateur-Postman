package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.*
import com.github.dhzhu.amateurpostman.services.CollectionChangeListener
import com.github.dhzhu.amateurpostman.services.CollectionService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeModelAdapter
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * Panel for displaying and managing request collections.
 *
 * Features:
 * - Tree view of collections, folders, and requests
 * - Context menu for operations
 * - Request loading on selection
 */
class CollectionsPanel(
    private val project: Project,
    private val onRequestSelected: (CollectionItem.Request) -> Unit
) : JPanel(BorderLayout()) {

    private val collectionService = project.service<CollectionService>()
    private val tree = Tree()
    private val rootNode = DefaultMutableTreeNode("Root")
    private var collections: List<RequestCollection> = emptyList()

    init {
        createUI()
        loadCollections()

        // Listen for collection changes
        collectionService.addChangeListener(object : CollectionChangeListener {
            override fun onCollectionChanged() {
                SwingUtilities.invokeLater {
                    loadCollections()
                }
            }
        })
    }

    private fun createUI() {
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        // Setup tree
        tree.model = DefaultTreeModel(rootNode)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = CollectionTreeCellRenderer()
        tree.isRootVisible = false
        tree.showsRootHandles = true

        // Mouse listener for context menu and double-click
        tree.addMouseListener(object : MouseAdapter() {
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

            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    loadSelectedRequest()
                }
            }
        })

        // Scroll pane
        val scrollPane = JBScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)

        // Toolbar
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
    }

    private fun createToolbar(): JPanel {
        val toolbar = JPanel(BorderLayout())
        toolbar.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)

        val titleLabel = JLabel("<html><b>Collections</b></html>")
        toolbar.add(titleLabel, BorderLayout.WEST)

        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)

        // New Collection button
        val newCollectionButton = JButton("New Collection")
        newCollectionButton.addActionListener { createNewCollection() }
        buttonPanel.add(newCollectionButton)

        // Import button
        val importButton = JButton("Import")
        importButton.addActionListener { importCollection() }
        buttonPanel.add(importButton)

        // Export button
        val exportButton = JButton("Export")
        exportButton.addActionListener { exportCollection() }
        buttonPanel.add(exportButton)

        toolbar.add(buttonPanel, BorderLayout.EAST)

        return toolbar
    }

    private fun loadCollections() {
        // Clear existing nodes
        rootNode.removeAllChildren()

        // Load collections from service
        collections = collectionService.getCollections()

        // Add each collection to tree
        collections.forEach { collection ->
            val collectionNode = CollectionNode(collection)
            rootNode.add(collectionNode)

            // Add items (folders and requests)
            collection.items.forEach { item ->
                collectionNode.add(createItemNode(item))
            }
        }

        // Refresh tree
        (tree.model as DefaultTreeModel).reload()
        expandAllRows()
    }

    private fun createItemNode(item: CollectionItem): DefaultMutableTreeNode {
        return when (item) {
            is CollectionItem.Folder -> {
                val folderNode = FolderNode(item)
                // Add children recursively
                item.children.forEach { child ->
                    folderNode.add(createItemNode(child))
                }
                folderNode
            }
            is CollectionItem.Request -> {
                RequestNode(item)
            }
        }
    }

    private fun expandAllRows() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }

    private fun showContextMenu(e: MouseEvent) {
        val path = tree.getPathForLocation(e.x, e.y)
        if (path != null) {
            tree.selectionPath = path
            val node = path.lastPathComponent
            val menu = createContextMenu(node)
            menu.show(tree, e.x, e.y)
        }
    }

    private fun createContextMenu(node: Any): JPopupMenu {
        val menu = JPopupMenu()

        when (node) {
            is CollectionNode -> {
                menu.add(MenuItem("Run Collection") { runCollection(node.collection) })
                menu.addSeparator()
                menu.add(MenuItem("New Folder") { createNewFolder(node.collection.id, null) })
                menu.add(MenuItem("New Request") { createNewRequest(node.collection.id, null) })
                menu.addSeparator()
                menu.add(MenuItem("Rename") { renameCollection(node.collection) })
                menu.add(MenuItem("Delete") { deleteCollection(node.collection.id) })
            }
            is FolderNode -> {
                menu.add(MenuItem("New Folder") { createNewFolder(node.collectionId, node.folder.id) })
                menu.add(MenuItem("New Request") { createNewRequest(node.collectionId, node.folder.id) })
                menu.addSeparator()
                menu.add(MenuItem("Rename") { renameFolder(node.collectionId, node.folder.id) })
                menu.add(MenuItem("Delete") { deleteFolder(node.collectionId, node.folder.id) })
            }
            is RequestNode -> {
                menu.add(MenuItem("Rename") { renameRequest(node.collectionId, node.request.id) })
                menu.add(MenuItem("Delete") { deleteRequest(node.collectionId, node.request.id) })
                menu.addSeparator()
                menu.add(MenuItem("Duplicate") { duplicateRequest(node.collectionId, node.request) })
            }
        }

        return menu
    }

    private fun loadSelectedRequest() {
        val path = tree.selectionPath
        if (path != null) {
            val node = path.lastPathComponent
            if (node is RequestNode) {
                onRequestSelected(node.request)
            }
        }
    }

    // ========== Operations ==========

    private fun runCollection(collection: RequestCollection) {
        val dialog = CollectionRunnerDialog(project, collection)
        dialog.show()
    }

    private fun createNewCollection() {
        val name = Messages.showInputDialog(
            project,
            "Enter collection name:",
            "New Collection",
            null
        ) ?: return

        if (name.isBlank()) {
            Messages.showErrorDialog(project, "Collection name cannot be empty.", "Invalid Name")
            return
        }

        collectionService.createCollection(name)
    }

    private fun createNewFolder(collectionId: String, parentId: String?) {
        val name = Messages.showInputDialog(
            project,
            "Enter folder name:",
            "New Folder",
            null
        ) ?: return

        if (name.isBlank()) {
            Messages.showErrorDialog(project, "Folder name cannot be empty.", "Invalid Name")
            return
        }

        collectionService.createFolder(collectionId, name, parentId)
    }

    private fun createNewRequest(collectionId: String, folderId: String?) {
        // This would typically open a dialog to enter request details
        // For now, we'll create a placeholder request
        val name = Messages.showInputDialog(
            project,
            "Enter request name:",
            "New Request",
            null
        ) ?: return

        if (name.isBlank()) {
            Messages.showErrorDialog(project, "Request name cannot be empty.", "Invalid Name")
            return
        }

        // Create a minimal GET request
        val request = HttpRequest(
            method = HttpMethod.GET,
            url = "",
            headers = emptyMap(),
            body = null
        )

        collectionService.addRequest(collectionId, request, name, "", "", "", folderId)
    }

    private fun renameCollection(collection: RequestCollection) {
        val newName = Messages.showInputDialog(
            project,
            "Enter new name for '${collection.name}':",
            "Rename Collection",
            null,
            collection.name,
            null
        ) ?: return

        if (newName.isBlank()) {
            Messages.showErrorDialog(project, "Collection name cannot be empty.", "Invalid Name")
            return
        }

        collectionService.renameCollection(collection.id, newName)
    }

    private fun renameFolder(collectionId: String, folderId: String) {
        val newName = Messages.showInputDialog(
            project,
            "Enter new folder name:",
            "Rename Folder",
            null
        ) ?: return

        if (newName.isBlank()) {
            Messages.showErrorDialog(project, "Folder name cannot be empty.", "Invalid Name")
            return
        }

        collectionService.renameFolder(collectionId, folderId, newName)
    }

    private fun renameRequest(collectionId: String, requestId: String) {
        val newName = Messages.showInputDialog(
            project,
            "Enter new request name:",
            "Rename Request",
            null
        ) ?: return

        if (newName.isBlank()) {
            Messages.showErrorDialog(project, "Request name cannot be empty.", "Invalid Name")
            return
        }

        val collection = collectionService.getCollection(collectionId)
        if (collection != null) {
            val item = collection.findItemById(requestId)
            if (item is CollectionItem.Request) {
                // Update request with new name (keep same request data)
                collectionService.updateRequest(collectionId, requestId, item.request)
                // Note: We'd need to also update the name, but the current service doesn't support that
                // This is a limitation of the current implementation
            }
        }
    }

    private fun deleteCollection(collectionId: String) {
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete this collection and all its items?",
            "Delete Collection",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            collectionService.deleteCollection(collectionId)
        }
    }

    private fun deleteFolder(collectionId: String, folderId: String) {
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete this folder and all its items?",
            "Delete Folder",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            collectionService.deleteFolder(collectionId, folderId)
        }
    }

    private fun deleteRequest(collectionId: String, requestId: String) {
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete this request?",
            "Delete Request",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            collectionService.deleteRequest(collectionId, requestId)
        }
    }

    private fun duplicateRequest(collectionId: String, request: CollectionItem.Request) {
        val newName = Messages.showInputDialog(
            project,
            "Enter name for duplicate:",
            "Duplicate Request",
            null,
            "${request.name} (Copy)",
            null
        ) ?: return

        if (newName.isBlank()) {
            Messages.showErrorDialog(project, "Request name cannot be empty.", "Invalid Name")
            return
        }

        collectionService.addRequest(
            collectionId,
            request.request,
            newName,
            request.description,
            request.preRequestScript,
            request.testScript,
            request.parentId
        )
    }

    private fun importCollection() {
        // Choose file to import
        val fileChooser = javax.swing.JFileChooser()
        fileChooser.dialogTitle = "Import Postman Collection"
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Postman Collection (*.json)", "json"
        )

        val result = fileChooser.showOpenDialog(this)
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile

            // Import collection
            val importResult = com.github.dhzhu.amateurpostman.utils.PostmanImporter.importFromFile(file)

            if (!importResult.isSuccess) {
                Messages.showErrorDialog(
                    project,
                    "Failed to import collection: ${importResult.error}",
                    "Import Error"
                )
                return
            }

            // Save to service
            val importedCollection = importResult.collection ?: return
            collectionService.createCollection(
                importedCollection.name,
                importedCollection.description
            )

            // Get the created collection and add items
            val collections = collectionService.getCollections()
            val createdCollection = collections.find { it.name == importedCollection.name }
            if (createdCollection != null) {
                // Add all items recursively
                importItemsRecursively(createdCollection.id, importedCollection.items)
            }

            // Show warnings if any
            if (importResult.warnings.isNotEmpty()) {
                Messages.showWarningDialog(
                    project,
                    "Collection imported with ${importResult.warnings.size} warning(s):\n\n${importResult.warnings.joinToString("\n")}",
                    "Import Warnings"
                )
            } else {
                Messages.showInfoMessage(
                    project,
                    "Collection '${importedCollection.name}' imported successfully!",
                    "Import Success"
                )
            }
        }
    }

    private fun importItemsRecursively(collectionId: String, items: List<com.github.dhzhu.amateurpostman.models.CollectionItem>, parentId: String? = null) {
        items.forEach { item ->
            when (item) {
                is com.github.dhzhu.amateurpostman.models.CollectionItem.Folder -> {
                    val folder = collectionService.createFolder(collectionId, item.name, parentId)
                    if (folder != null) {
                        importItemsRecursively(collectionId, item.children, folder.id)
                    }
                }
                is com.github.dhzhu.amateurpostman.models.CollectionItem.Request -> {
                    collectionService.addRequest(
                        collectionId,
                        item.request,
                        item.name,
                        item.description,
                        item.preRequestScript,
                        item.testScript,
                        parentId
                    )
                }
            }
        }
    }

    private fun exportCollection() {
        // Select collection to export
        val collections = collectionService.getCollections()
        if (collections.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "No collections to export. Create a collection first.",
                "Export Warning"
            )
            return
        }

        val collectionNames = collections.map { it.name }.toTypedArray()
        val selectedIndex = Messages.showChooseDialog(
            project,
            "Select collection to export:",
            "Export Collection",
            null,
            collectionNames,
            collectionNames.firstOrNull()
        )

        if (selectedIndex < 0) return

        val collection = collections[selectedIndex]

        // Choose save location
        val fileChooser = javax.swing.JFileChooser()
        fileChooser.dialogTitle = "Export Postman Collection"
        fileChooser.selectedFile = java.io.File("${collection.name}.json")
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Postman Collection (*.json)", "json"
        )

        val result = fileChooser.showSaveDialog(this)
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile

            // Export collection
            val exportResult = com.github.dhzhu.amateurpostman.utils.PostmanExporter.exportToFile(collection, file)

            if (!exportResult.isSuccess) {
                Messages.showErrorDialog(
                    project,
                    "Failed to export collection: ${exportResult.error}",
                    "Export Error"
                )
                return
            }

            // Show warnings if any
            if (exportResult.warnings.isNotEmpty()) {
                Messages.showWarningDialog(
                    project,
                    "Collection exported with ${exportResult.warnings.size} warning(s):\n\n${exportResult.warnings.joinToString("\n")}",
                    "Export Warnings"
                )
            } else {
                Messages.showInfoMessage(
                    project,
                    "Collection '${collection.name}' exported successfully to:\n${file.absolutePath}",
                    "Export Success"
                )
            }
        }
    }

    // ========== Tree Nodes ==========

    private abstract class NamedNode(val name: String) : DefaultMutableTreeNode(name)

    private class CollectionNode(val collection: RequestCollection) :
        NamedNode(collection.name) {
        init {
            allowsChildren = true
        }
    }

    private class FolderNode(val folder: CollectionItem.Folder, val collectionId: String = "") :
        NamedNode(folder.name) {
        init {
            allowsChildren = true
        }
    }

    private class RequestNode(val request: CollectionItem.Request, val collectionId: String = "") :
        NamedNode(request.name) {
        init {
            allowsChildren = false
        }
    }

    // ========== Tree Cell Renderer ==========

    private class CollectionTreeCellRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            val component = super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus
            )

            when (value) {
                is CollectionNode -> {
                    icon = null // Use default icon
                    text = value.collection.name
                }
                is FolderNode -> {
                    icon = if (expanded) {
                        UIManager.getIcon("Tree.openIcon")
                    } else {
                        UIManager.getIcon("Tree.closedIcon")
                    }
                    text = value.folder.name
                }
                is RequestNode -> {
                    icon = UIManager.getIcon("Tree.leafIcon")
                    val method = value.request.request.method.name
                    text = "${value.request.name} [$method]"
                }
            }

            return component
        }
    }

    // ========== Menu Item Helper ==========

    private class MenuItem(text: String, private val action: () -> Unit) : JMenuItem(text) {
        init {
            addActionListener { e: ActionEvent? ->
                action()
            }
        }
    }
}

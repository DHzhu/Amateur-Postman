package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.services.CollectionService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.event.ActionEvent
import javax.swing.SwingUtilities

/**
 * Simplified dialog for saving a request to a collection.
 */
class SaveRequestDialog(
    private val project: Project,
    private val request: HttpRequest,
    private val onSave: (collectionId: String, folderId: String?, name: String, description: String) -> Unit
) : com.intellij.openapi.ui.DialogWrapper(true) {

    private val collectionService = project.service<CollectionService>()
    private val collections = collectionService.getCollections()

    override fun createCenterPanel(): com.intellij.openapi.ui.DialogPanel {
        val formPanel = com.intellij.openapi.ui.DialogPanel()
        formPanel.border = JBUI.Borders.empty(10)

        // Name field
        val nameField = JBTextField()
        val defaultName = request.url.substringAfterLast("/").substringBefore("?").take(30)
        nameField.text = defaultName.ifEmpty { request.url.take(30) }
        formPanel.add(JBLabel("Request Name:"), 0)
        formPanel.add(nameField, 0)

        // Description
        val descArea = JBTextArea(3, 40)
        formPanel.add(JBLabel("Description:"), 0)
        formPanel.add(JBScrollPane(descArea), 0)

        // Collection selector
        val collectionCombo = javax.swing.JComboBox<String>()
        collections.forEach { collection ->
            collectionCombo.addItem(collection.name)
        }
        if (collections.isEmpty()) {
            collectionCombo.addItem("No Collections")
            collectionCombo.isEnabled = false
        }
        formPanel.add(JBLabel("Collection:"), 0)
        formPanel.add(collectionCombo, 0)

        // Buttons
        val saveButton = javax.swing.JButton("Save")
        val cancelButton = javax.swing.JButton("Cancel")

        saveButton.addActionListener {
            val name = nameField.text.trim()
            if (name.isEmpty()) {
                Messages.showErrorDialog("Please enter a request name.", "Validation Error")
                return@addActionListener
            }

            val selectedIndex = collectionCombo.selectedIndex
            if (selectedIndex < 0 || selectedIndex >= collections.size) {
                return@addActionListener
            }

            val collection = collections[selectedIndex]
            val description = descArea.text.trim()

            onSave(collection.id, null, name, description)
            dispose()
        }

        cancelButton.addActionListener { dispose() }

        formPanel.add(saveButton, 0)
        formPanel.add(cancelButton, 0)

        return formPanel
    }

    companion object {
        fun show(
            project: Project,
            request: HttpRequest,
            onSave: (collectionId: String, folderId: String?, name: String, description: String) -> Unit
        ) {
            SwingUtilities.invokeLater {
                val dialog = SaveRequestDialog(project, request, onSave)
                dialog.show()
            }
        }
    }
}

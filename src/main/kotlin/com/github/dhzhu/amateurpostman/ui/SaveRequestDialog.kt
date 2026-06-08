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
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Dialog for saving or updating a request in a collection.
 *
 * Supports two modes:
 * - New save: user picks a collection and enters a name
 * - Update mode: pre-fills from an existing request, offers Update / Save as New / Cancel
 */
class SaveRequestDialog(
    private val project: Project,
    private val request: HttpRequest,
    private val existingRequestName: String? = null,
    private val existingCollectionId: String? = null,
    private val onSave: (collectionId: String, folderId: String?, name: String, description: String) -> Unit,
    private val onUpdate: (() -> Unit)? = null
) : com.intellij.openapi.ui.DialogWrapper(true) {

    private val collectionService = project.service<CollectionService>()
    private val collections = collectionService.getCollections()

    private lateinit var nameField: JBTextField
    private lateinit var descArea: JBTextArea
    private lateinit var collectionCombo: javax.swing.JComboBox<String>

    init {
        title = if (existingRequestName != null) "Update Request" else "Save Request"
        init()
    }

    override fun createCenterPanel(): com.intellij.openapi.ui.DialogPanel {
        val panel = com.intellij.openapi.ui.DialogPanel()
        panel.layout = BorderLayout()
        panel.border = JBUI.Borders.empty(10)

        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(4)

        // Row 0: Request Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        formPanel.add(JBLabel("Request Name:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        nameField = JBTextField()
        nameField.text = existingRequestName
            ?: request.url.substringAfterLast("/").substringBefore("?").take(30).ifEmpty { request.url.take(30) }
        formPanel.add(nameField, gbc)

        // Row 1: Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.NORTHWEST
        formPanel.add(JBLabel("Description:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH
        descArea = JBTextArea(3, 40)
        descArea.lineWrap = true
        descArea.wrapStyleWord = true
        formPanel.add(JBScrollPane(descArea), gbc)

        // Row 2: Collection selector
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL
        formPanel.add(JBLabel("Collection:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        collectionCombo = javax.swing.JComboBox<String>()
        collections.forEach { collection ->
            collectionCombo.addItem(collection.name)
        }
        if (collections.isEmpty()) {
            collectionCombo.addItem("No Collections")
            collectionCombo.isEnabled = false
        }
        // Pre-select the existing collection
        if (existingCollectionId != null) {
            val idx = collections.indexOfFirst { it.id == existingCollectionId }
            if (idx >= 0) collectionCombo.selectedIndex = idx
        }
        formPanel.add(collectionCombo, gbc)

        panel.add(formPanel, BorderLayout.CENTER)
        return panel
    }

    override fun createActions(): Array<javax.swing.Action> {
        if (existingRequestName != null && onUpdate != null) {
            // Update mode: show Update / Save as New / Cancel
            val updateAction = object : com.intellij.openapi.ui.DialogWrapper.DialogWrapperAction("Update") {
                override fun doAction(e: java.awt.event.ActionEvent) {
                    onUpdate.invoke()
                    close(OK_EXIT_CODE)
                }
            }
            val saveAsNewAction = object : com.intellij.openapi.ui.DialogWrapper.DialogWrapperAction("Save as New") {
                override fun doAction(e: java.awt.event.ActionEvent) {
                    doSaveAsNew()
                }
            }
            return arrayOf(updateAction, saveAsNewAction, cancelAction)
        }
        // New save mode: show OK / Cancel
        return arrayOf(okAction, cancelAction)
    }

    override fun doOKAction() {
        doSaveAsNew()
    }

    private fun doSaveAsNew() {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            Messages.showErrorDialog(project, "Please enter a request name.", "Validation Error")
            return
        }

        val selectedIndex = collectionCombo.selectedIndex
        if (selectedIndex < 0 || selectedIndex >= collections.size) {
            Messages.showWarningDialog(project, "Please select a collection.", "Validation Error")
            return
        }

        val collection = collections[selectedIndex]
        val description = descArea.text.trim()

        onSave(collection.id, null, name, description)
        close(OK_EXIT_CODE)
    }

    companion object {
        /**
         * Show dialog for saving a new request.
         */
        fun show(
            project: Project,
            request: HttpRequest,
            onSave: (collectionId: String, folderId: String?, name: String, description: String) -> Unit
        ) {
            SwingUtilities.invokeLater {
                SaveRequestDialog(project, request, onSave = onSave).show()
            }
        }

        /**
         * Show dialog for updating an existing request (or saving as new).
         */
        fun showForUpdate(
            project: Project,
            request: HttpRequest,
            existingRequestName: String,
            existingCollectionId: String,
            onUpdate: () -> Unit,
            onSaveAsNew: (collectionId: String, folderId: String?, name: String, description: String) -> Unit
        ) {
            SwingUtilities.invokeLater {
                SaveRequestDialog(
                    project, request,
                    existingRequestName = existingRequestName,
                    existingCollectionId = existingCollectionId,
                    onSave = onSaveAsNew,
                    onUpdate = onUpdate
                ).show()
            }
        }
    }
}

package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.services.HttpRequestService
import com.github.dhzhu.amateurpostman.utils.CurlExporter
import com.github.dhzhu.amateurpostman.utils.CurlParser
import com.github.dhzhu.amateurpostman.utils.SyntaxHighlighter
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.table.DefaultTableModel
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

/** Main panel for the Amateur-Postman tool window */
class PostmanToolWindowPanel(private val project: Project) : Disposable {

    private val httpService = project.service<HttpRequestService>()
    private val historyService =
            project.service<com.github.dhzhu.amateurpostman.services.RequestHistoryService>()
    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    // Request cancellation tracking
    private var currentRequestJob: Job? = null
    private var isRequestInProgress = false

    // UI State
    private var selectedMethod = HttpMethod.GET
    private var statusText = "Ready"

    // UI Components
    private lateinit var urlField: JBTextField
    private lateinit var methodComboBox: ComboBox<HttpMethod>
    private lateinit var sendButton: JButton

    // Tabs
    private lateinit var tabbedPane: JBTabbedPane
    private lateinit var paramsTable: JBTable
    private lateinit var headersTable: JBTable
    private lateinit var requestBodyArea: JBTextArea
    private lateinit var authPanel: JPanel

    // Auth Components
    private lateinit var authTypeComboBox: ComboBox<String>
    private lateinit var authContentPanel: JPanel
    private lateinit var basicAuthUserField: JBTextField
    private lateinit var basicAuthPassField: JPasswordField
    private lateinit var bearerTokenField: JBTextField

    // Response with syntax highlighting
    private lateinit var responseTextPane: JTextPane
    private lateinit var responseTabbedPane: JBTabbedPane
    private lateinit var responseHeadersArea: JBTextArea
    private lateinit var responseRawArea: JBTextArea
    private lateinit var statusLabel: JLabel
    private lateinit var responseSizeLabel: JLabel

    // History
    private lateinit var historyPanel: HistoryPanel

    private val headersTableModel = DefaultTableModel(arrayOf("Key", "Value"), 0)
    private val paramsTableModel = DefaultTableModel(arrayOf("Key", "Value", "Description"), 0)

    fun createPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())

        // 1. Top Bar: Method | URL | Send
        val topPanel = JPanel(BorderLayout(5, 0))
        topPanel.border = JBUI.Borders.empty(5)

        methodComboBox = ComboBox(HttpMethod.entries.toTypedArray())
        methodComboBox.selectedItem = selectedMethod
        methodComboBox.addActionListener {
            selectedMethod = methodComboBox.selectedItem as HttpMethod
        }

        urlField = JBTextField()
        sendButton = JButton("Send")
        sendButton.addActionListener { sendRequest() }

        topPanel.add(methodComboBox, BorderLayout.WEST)
        topPanel.add(urlField, BorderLayout.CENTER)
        topPanel.add(sendButton, BorderLayout.EAST)

        // cURL buttons
        val curlButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        val importCurlButton = JButton("Import cURL")
        importCurlButton.addActionListener { importCurl() }
        val exportCurlButton = JButton("Export cURL")
        exportCurlButton.addActionListener { exportCurl() }
        curlButtonPanel.add(importCurlButton)
        curlButtonPanel.add(exportCurlButton)

        val topContainer = JPanel(BorderLayout())
        topContainer.add(topPanel, BorderLayout.NORTH)
        topContainer.add(curlButtonPanel, BorderLayout.SOUTH)

        mainPanel.add(topContainer, BorderLayout.NORTH)

        // 2. Center: Main content with left sidebar for history
        val mainContentPanel = JPanel(BorderLayout())

        // Left sidebar: History Panel
        historyPanel = HistoryPanel(project) { entry -> loadFromHistory(entry) }
        historyPanel.preferredSize = java.awt.Dimension(250, 0)

        // Request/Response area
        val requestResponsePanel = JPanel(BorderLayout())

        // Request Tabs
        tabbedPane = JBTabbedPane()

        // Tab: Params
        paramsTable = JBTable(paramsTableModel)
        paramsTable.setShowGrid(true)
        val paramsPanel = createTablePanel(paramsTable, paramsTableModel)
        tabbedPane.addTab("Params", paramsPanel)

        // Tab: Authorization
        authPanel = createAuthPanel()
        tabbedPane.addTab("Authorization", authPanel)

        // Tab: Headers
        headersTable = JBTable(headersTableModel)
        headersTable.setShowGrid(true)
        headersTableModel.addRow(arrayOf("Content-Type", "application/json")) // Default header
        val headersPanel = createTablePanel(headersTable, headersTableModel)
        tabbedPane.addTab("Headers", headersPanel)

        // Tab: Body with format button
        val bodyPanel = JPanel(BorderLayout())
        requestBodyArea = createTextArea()

        val bodyToolbar = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
        val formatJsonButton = JButton("Format JSON")
        formatJsonButton.addActionListener { formatRequestBody() }
        bodyToolbar.add(formatJsonButton)

        bodyPanel.add(bodyToolbar, BorderLayout.NORTH)
        bodyPanel.add(JBScrollPane(requestBodyArea), BorderLayout.CENTER)
        tabbedPane.addTab("Body", bodyPanel)

        // Response Area with tabs for Headers/Body/Raw
        val responsePanel = JPanel(BorderLayout())

        val statusPanel = JPanel(BorderLayout())
        statusLabel = JLabel(statusText)
        statusLabel.border = JBUI.Borders.empty(5)
        responseSizeLabel = JLabel("")
        responseSizeLabel.border = JBUI.Borders.empty(5)

        val copyResponseButton = JButton("Copy")
        copyResponseButton.addActionListener { copyResponse() }

        val clearResponseButton = JButton("Clear")
        clearResponseButton.addActionListener { clearResponse() }

        val responseButtonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        responseButtonPanel.add(responseSizeLabel)
        responseButtonPanel.add(copyResponseButton)
        responseButtonPanel.add(clearResponseButton)

        statusPanel.add(statusLabel, BorderLayout.WEST)
        statusPanel.add(responseButtonPanel, BorderLayout.EAST)

        // Response tabs
        responseTabbedPane = JBTabbedPane()

        // Body tab with syntax highlighting
        responseTextPane = JTextPane()
        responseTextPane.isEditable = false
        responseTextPane.font = Font("Monospaced", Font.PLAIN, 12)
        responseTextPane.background = Color(43, 43, 43)
        responseTextPane.foreground = Color(212, 212, 212)
        responseTabbedPane.addTab("Body", JBScrollPane(responseTextPane))

        // Headers tab
        responseHeadersArea = createTextArea()
        responseHeadersArea.isEditable = false
        responseTabbedPane.addTab("Headers", JBScrollPane(responseHeadersArea))

        // Raw tab
        responseRawArea = createTextArea()
        responseRawArea.isEditable = false
        responseTabbedPane.addTab("Raw", JBScrollPane(responseRawArea))

        responsePanel.add(statusPanel, BorderLayout.NORTH)
        responsePanel.add(responseTabbedPane, BorderLayout.CENTER)

        // Split Pane (Request Tabs vs Response)
        val splitPane = com.intellij.ui.JBSplitter(true, 0.5f)
        splitPane.dividerWidth = 2
        splitPane.divider.background = com.intellij.util.ui.UIUtil.getPanelBackground().darker()

        splitPane.firstComponent = tabbedPane
        splitPane.secondComponent = responsePanel

        requestResponsePanel.add(splitPane, BorderLayout.CENTER)

        // Horizontal splitter for history and request/response
        val horizontalSplitter = com.intellij.ui.JBSplitter(false, 0.25f)
        horizontalSplitter.firstComponent = historyPanel
        horizontalSplitter.secondComponent = requestResponsePanel

        mainContentPanel.add(horizontalSplitter, BorderLayout.CENTER)
        mainPanel.add(mainContentPanel, BorderLayout.CENTER)

        // Setup keyboard shortcuts
        setupKeyboardShortcuts(mainPanel)

        return mainPanel
    }

    private fun setupKeyboardShortcuts(panel: JPanel) {
        val inputMap = panel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
        val actionMap = panel.actionMap

        // Ctrl+Enter: Send request
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                "sendRequest"
        )
        actionMap.put(
                "sendRequest",
                object : AbstractAction() {
                    override fun actionPerformed(e: ActionEvent) {
                        sendRequest()
                    }
                }
        )

        // Ctrl+L: Clear response
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK),
                "clearResponse"
        )
        actionMap.put(
                "clearResponse",
                object : AbstractAction() {
                    override fun actionPerformed(e: ActionEvent) {
                        clearResponse()
                    }
                }
        )

        // Escape: Cancel request
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelRequest")
        actionMap.put(
                "cancelRequest",
                object : AbstractAction() {
                    override fun actionPerformed(e: ActionEvent) {
                        if (isRequestInProgress) {
                            currentRequestJob?.cancel()
                            isRequestInProgress = false
                            sendButton.text = "Send"
                            statusLabel.text = "Request cancelled"
                        }
                    }
                }
        )
    }

    private fun formatRequestBody() {
        try {
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val jsonElement = com.google.gson.JsonParser.parseString(requestBodyArea.text)
            requestBodyArea.text = gson.toJson(jsonElement)
        } catch (e: Exception) {
            Messages.showWarningDialog(project, "Invalid JSON: ${e.message}", "Format Error")
        }
    }

    private fun copyResponse() {
        val text = responseTextPane.text
        if (text.isNotEmpty()) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            statusLabel.text = "Response copied to clipboard"
        }
    }

    private fun clearResponse() {
        responseTextPane.text = ""
        responseHeadersArea.text = ""
        responseRawArea.text = ""
        responseSizeLabel.text = ""
        statusLabel.text = "Ready"
    }

    private fun loadFromHistory(entry: com.github.dhzhu.amateurpostman.models.RequestHistoryEntry) {
        urlField.text = entry.request.url
        methodComboBox.selectedItem = entry.request.method
        selectedMethod = entry.request.method

        // Clear and set headers
        while (headersTableModel.rowCount > 0) {
            headersTableModel.removeRow(0)
        }
        entry.request.headers.forEach { (key, value) ->
            headersTableModel.addRow(arrayOf(key, value))
        }

        // Set body
        requestBodyArea.text = entry.request.body ?: ""

        statusLabel.text = "Loaded from history: ${entry.getDisplayName()}"
    }

    private fun createTablePanel(table: JBTable, model: DefaultTableModel): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(JBScrollPane(table), BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add")
        addButton.addActionListener {
            model.addRow(arrayOf("", "", "")) // Add empty row
            val newRow = model.rowCount - 1
            table.setRowSelectionInterval(newRow, newRow)
            table.scrollRectToVisible(table.getCellRect(newRow, 0, true))
        }
        val removeButton = JButton("Remove")
        removeButton.addActionListener {
            val selectedRow = table.selectedRow
            if (selectedRow >= 0) {
                model.removeRow(selectedRow)
                if (model.rowCount > 0) {
                    // Select the previous row, or the first one if we deleted the first
                    val newSelection = if (selectedRow > 0) selectedRow - 1 else 0
                    table.setRowSelectionInterval(newSelection, newSelection)
                }
            }
        }
        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)

        panel.add(buttonPanel, BorderLayout.SOUTH)
        return panel
    }

    private fun createAuthPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val c = GridBagConstraints()
        c.insets = JBUI.insets(5)
        c.anchor = GridBagConstraints.WEST
        c.fill = GridBagConstraints.HORIZONTAL

        // Auth Type Selector
        c.gridx = 0
        c.gridy = 0
        panel.add(JLabel("Type:"), c)

        val authTypes = arrayOf("No Auth", "Basic Auth", "Bearer Token")
        authTypeComboBox = ComboBox(authTypes)
        c.gridx = 1
        c.weightx = 1.0
        panel.add(authTypeComboBox, c)

        // Dynamic Content Panel
        authContentPanel = JPanel(GridBagLayout())
        c.gridx = 0
        c.gridy = 1
        c.gridwidth = 2
        c.weighty = 1.0
        c.fill = GridBagConstraints.BOTH
        panel.add(authContentPanel, c)

        // Initialize Fields
        basicAuthUserField = JBTextField()
        basicAuthPassField = JPasswordField()
        bearerTokenField = JBTextField()

        authTypeComboBox.addActionListener { updateAuthPanel() }

        updateAuthPanel() // Initial state

        return panel
    }

    private fun updateAuthPanel() {
        authContentPanel.removeAll()
        val type = authTypeComboBox.selectedItem as String
        val c = GridBagConstraints()
        c.insets = JBUI.insets(5)
        c.anchor = GridBagConstraints.WEST
        c.fill = GridBagConstraints.HORIZONTAL

        when (type) {
            "Basic Auth" -> {
                c.gridx = 0
                c.gridy = 0
                authContentPanel.add(JLabel("Username:"), c)
                c.gridx = 1
                c.weightx = 1.0
                authContentPanel.add(basicAuthUserField, c)

                c.gridx = 0
                c.gridy = 1
                c.weightx = 0.0
                authContentPanel.add(JLabel("Password:"), c)
                c.gridx = 1
                c.weightx = 1.0
                authContentPanel.add(basicAuthPassField, c)
            }
            "Bearer Token" -> {
                c.gridx = 0
                c.gridy = 0
                authContentPanel.add(JLabel("Token:"), c)
                c.gridx = 1
                c.weightx = 1.0
                authContentPanel.add(bearerTokenField, c)
            }
        }
        authContentPanel.revalidate()
        authContentPanel.repaint()
    }

    private fun createTextArea(): JBTextArea {
        return JBTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            font = Font("Monospaced", Font.PLAIN, 12)
        }
    }

    private fun importCurl() {
        val curlCommand =
                Messages.showMultilineInputDialog(
                        project,
                        "Paste cURL command:",
                        "Import cURL",
                        "",
                        Messages.getQuestionIcon(),
                        null
                )

        if (curlCommand.isNullOrBlank()) return

        try {
            val request = CurlParser.parse(curlCommand)

            // Fill in the UI with parsed request
            urlField.text = request.url
            methodComboBox.selectedItem = request.method

            // Clear and set headers
            while (headersTableModel.rowCount > 0) {
                headersTableModel.removeRow(0)
            }
            request.headers.forEach { (key, value) ->
                headersTableModel.addRow(arrayOf(key, value))
            }

            // Set body
            requestBodyArea.text = request.body ?: ""

            statusLabel.text = "cURL imported successfully"
        } catch (e: Exception) {
            Messages.showErrorDialog(
                    project,
                    "Failed to parse cURL command: ${e.message}",
                    "Import Error"
            )
        }
    }

    private fun exportCurl() {
        // Build current request from UI
        val headers = mutableMapOf<String, String>()
        for (i in 0 until headersTableModel.rowCount) {
            val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
            val value = headersTableModel.getValueAt(i, 1)?.toString()?.trim()
            if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                headers[key] = value
            }
        }

        // Handle Auth for export
        val authType = authTypeComboBox.selectedItem as String
        when (authType) {
            "Basic Auth" -> {
                val username = basicAuthUserField.text.trim()
                val password = String(basicAuthPassField.password)
                if (username.isNotEmpty() || password.isNotEmpty()) {
                    val credentials = "$username:$password"
                    val encoded =
                            java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
                    headers["Authorization"] = "Basic $encoded"
                }
            }
            "Bearer Token" -> {
                val token = bearerTokenField.text.trim()
                if (token.isNotEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
            }
        }

        val request =
                HttpRequest(
                        url = urlField.text.trim(),
                        method = selectedMethod,
                        headers = headers,
                        body =
                                if (requestBodyArea.text.isNotBlank()) requestBodyArea.text
                                else null,
                        contentType = headers["Content-Type"]
                )

        val curlCommand = CurlExporter.exportWithOptions(request, multiLine = true)

        // Copy to clipboard
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(curlCommand), null)

        // Show in a dialog
        Messages.showInfoMessage(
                project,
                "cURL command copied to clipboard:\n\n$curlCommand",
                "Export cURL"
        )
    }

    private fun sendRequest() {
        // If request is in progress, cancel it
        if (isRequestInProgress) {
            currentRequestJob?.cancel()
            isRequestInProgress = false
            sendButton.text = "Send"
            statusLabel.text = "Request cancelled"
            return
        }

        // Validate URL is not empty
        val urlText = urlField.text.trim()
        if (urlText.isEmpty()) {
            statusLabel.text = "Error: URL cannot be empty"
            return
        }

        // Validate URL format
        try {
            URI(urlText).toURL()
        } catch (e: Exception) {
            statusLabel.text = "Error: Invalid URL format"
            return
        }

        statusLabel.text = "Sending request..."
        responseTextPane.text = ""
        isRequestInProgress = true
        sendButton.text = "Cancel"

        currentRequestJob =
                scope.launch {
                    try {
                        // Collect headers
                        val headers = mutableMapOf<String, String>()
                        for (i in 0 until headersTableModel.rowCount) {
                            val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
                            val value = headersTableModel.getValueAt(i, 1)?.toString()?.trim()
                            if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                                headers[key] = value
                            }
                        }

                        // Handle Auth
                        val authType = authTypeComboBox.selectedItem as String
                        when (authType) {
                            "Basic Auth" -> {
                                val username = basicAuthUserField.text.trim()
                                val password = String(basicAuthPassField.password)
                                if (username.isNotEmpty() || password.isNotEmpty()) {
                                    val credentials = "$username:$password"
                                    val encoded =
                                            java.util.Base64.getEncoder()
                                                    .encodeToString(credentials.toByteArray())
                                    headers["Authorization"] = "Basic $encoded"
                                }
                            }
                            "Bearer Token" -> {
                                val token = bearerTokenField.text.trim()
                                if (token.isNotEmpty()) {
                                    headers["Authorization"] = "Bearer $token"
                                }
                            }
                        }

                        // Handle Params (Append to URL)
                        var url = urlText
                        val params = mutableListOf<String>()
                        for (i in 0 until paramsTableModel.rowCount) {
                            val key = paramsTableModel.getValueAt(i, 0)?.toString()?.trim()
                            val value = paramsTableModel.getValueAt(i, 1)?.toString()?.trim()
                            if (!key.isNullOrEmpty()) {
                                val encodedKey =
                                        java.net.URLEncoder.encode(key, StandardCharsets.UTF_8)
                                val encodedValue =
                                        java.net.URLEncoder.encode(
                                                value ?: "",
                                                StandardCharsets.UTF_8
                                        )
                                params.add("$encodedKey=$encodedValue")
                            }
                        }

                        if (params.isNotEmpty()) {
                            val separator = if (url.contains("?")) "&" else "?"
                            url += separator + params.joinToString("&")
                        }

                        // Content Type
                        val contentType = headers["Content-Type"] ?: "application/json"

                        // Build request
                        val request =
                                HttpRequest(
                                        url = url,
                                        method = selectedMethod,
                                        headers = headers,
                                        body =
                                                if (requestBodyArea.text.isNotBlank())
                                                        requestBodyArea.text
                                                else null,
                                        contentType = contentType
                                )

                        // Execute
                        val response = httpService.executeRequest(request)

                        // Display
                        displayResponse(response)

                        // Save to history
                        historyService.addEntry(request, response)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Request was cancelled, already handled
                    } catch (e: Exception) {
                        statusLabel.text = "Error: ${e.message}"
                        responseTextPane.text =
                                "Error executing request:\n${e.message}\n\n${e.stackTraceToString()}"
                    } finally {
                        isRequestInProgress = false
                        sendButton.text = "Send"
                    }
                }
    }

    private fun displayResponse(response: HttpResponse) {
        statusLabel.text =
                "Status: ${response.statusCode} ${response.statusMessage} | Time: ${response.duration}ms"

        // Show response size
        val bodySize = response.body.toByteArray().size
        responseSizeLabel.text = formatSize(bodySize)

        // Populate Headers tab
        val headersText = buildString {
            response.headers.forEach { (key, values) ->
                values.forEach { value -> appendLine("$key: $value") }
            }
        }
        responseHeadersArea.text = headersText

        // Populate Raw tab
        val rawResponse = buildString {
            appendLine("HTTP ${response.statusCode} ${response.statusMessage}")
            response.headers.forEach { (key, values) ->
                values.forEach { value -> appendLine("$key: $value") }
            }
            appendLine()
            append(response.body)
        }
        responseRawArea.text = rawResponse

        // Populate Body tab with syntax highlighting
        val doc = responseTextPane.styledDocument
        doc.remove(0, doc.length)

        val body = formatResponseBody(response.body, response.headers)
        val contentType = response.headers["Content-Type"]?.firstOrNull() ?: ""

        if (contentType.contains("application/json", ignoreCase = true)) {
            // Apply JSON syntax highlighting
            val tokens = SyntaxHighlighter.highlightJsonToTokens(body)
            for (token in tokens) {
                val style = SimpleAttributeSet()
                val color =
                        when (token.type) {
                            SyntaxHighlighter.TokenType.KEY -> Color(156, 220, 254)
                            SyntaxHighlighter.TokenType.STRING -> Color(206, 145, 120)
                            SyntaxHighlighter.TokenType.NUMBER -> Color(181, 206, 168)
                            SyntaxHighlighter.TokenType.BOOLEAN, SyntaxHighlighter.TokenType.NULL ->
                                    Color(86, 156, 214)
                            else -> Color(212, 212, 212)
                        }
                StyleConstants.setForeground(style, color)
                doc.insertString(doc.length, token.text, style)
            }
        } else {
            val normalStyle = SimpleAttributeSet()
            StyleConstants.setForeground(normalStyle, Color(212, 212, 212))
            doc.insertString(doc.length, body, normalStyle)
        }
    }

    private fun formatSize(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    private fun formatResponseBody(body: String, headers: Map<String, List<String>>): String {
        val contentType = headers["Content-Type"]?.firstOrNull() ?: ""
        return if (contentType.contains("application/json", ignoreCase = true)) {
            try {
                formatJson(body)
            } catch (e: Exception) {
                body
            }
        } else {
            body
        }
    }

    private fun formatJson(json: String): String {
        return try {
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val jsonElement = com.google.gson.JsonParser.parseString(json)
            gson.toJson(jsonElement)
        } catch (e: Exception) {
            json
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

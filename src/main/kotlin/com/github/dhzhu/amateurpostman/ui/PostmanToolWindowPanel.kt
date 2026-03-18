package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.ApiKeyAuth
import com.github.dhzhu.amateurpostman.models.BodyType
import com.github.dhzhu.amateurpostman.models.GraphQLRequest
import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.models.MultipartPart
import com.github.dhzhu.amateurpostman.services.HttpRequestService
import com.github.dhzhu.amateurpostman.utils.CurlExporter
import com.github.dhzhu.amateurpostman.utils.CurlParser
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
import com.github.dhzhu.amateurpostman.ui.MockServerPanel
import com.github.dhzhu.amateurpostman.ui.QuickLookPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
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
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField
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
import javax.swing.SwingUtilities

/** Main panel for the Amateur-Postman tool window */
class PostmanToolWindowPanel(private val project: Project) : Disposable {

    private val httpService = project.service<HttpRequestService>()
    private val historyService =
            project.service<com.github.dhzhu.amateurpostman.services.RequestHistoryService>()
    private val scriptExecutionService = project.service<com.github.dhzhu.amateurpostman.services.ScriptExecutionService>()
    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    // Request cancellation tracking
    private var currentRequestJob: Job? = null
    private var isRequestInProgress = false

    // UI State
    private var selectedMethod = HttpMethod.GET
    private var selectedBodyType = BodyType.JSON
    private var statusText = "Ready"
    private var currentEditingRequestItem: com.github.dhzhu.amateurpostman.models.CollectionItem.Request? = null
    private var currentActiveCollectionId: String? = null

    // UI Components
    private lateinit var urlField: JBTextField
    private lateinit var methodComboBox: ComboBox<HttpMethod>
    private lateinit var sendButton: JButton
    private lateinit var bodyTypeComboBox: ComboBox<BodyType>

    // Multipart components
    private lateinit var multipartTable: JBTable
    private val multipartTableModel = DefaultTableModel(
        arrayOf("Key", "Type", "Value", "Content-Type", "Description"),
        0
    )
    private val multipartParts = mutableListOf<MultipartPart>()

    // GraphQL components
    private lateinit var graphqlPanel: GraphQLPanel

    // Tabs
    private lateinit var tabbedPane: JBTabbedPane
    private lateinit var paramsTable: JBTable
    private lateinit var headersTable: JBTable
    private lateinit var requestBodyArea: JBTextArea
    private lateinit var authPanelWrapper: AuthPanel

    // Response with syntax highlighting
    private lateinit var responseViewer: HighPerfResponseViewer
    private lateinit var responseTabbedPane: JBTabbedPane
    private lateinit var responseHeadersArea: JBTextArea
    private lateinit var responseRawArea: JBTextArea
    private lateinit var responseTestResultsArea: JBTextArea
    private lateinit var profilingPanel: ProfilingPanel
    private lateinit var statusLabel: JLabel
    private lateinit var responseSizeLabel: JLabel

    // History
    private lateinit var historyPanel: HistoryPanel

    // Environments
    private lateinit var environmentPanel: EnvironmentPanel

    // Collections
    private lateinit var collectionsPanel: CollectionsPanel
    private lateinit var mockServerPanel: MockServerPanel

    // Script panels
    private lateinit var preRequestScriptArea: JBTextArea
    private lateinit var testsScriptArea: JBTextArea

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
        val saveButton = JButton("Save")
        saveButton.addActionListener { saveRequest() }

        // Quick Look button
        val quickLookButton = JButton("🔍")  // Using magnifying glass emoji as icon
        quickLookButton.toolTipText = "Environment Quick Look"
        quickLookButton.addActionListener {
            val quickLookPanel = QuickLookPanel(project)
            quickLookPanel.show(quickLookButton, getCurrentCollectionId()) // Pass current collection ID if available
        }

        curlButtonPanel.add(importCurlButton)
        curlButtonPanel.add(exportCurlButton)
        curlButtonPanel.add(saveButton)
        curlButtonPanel.add(quickLookButton)  // Add the Quick Look button

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
        authPanelWrapper = AuthPanel(project)
        tabbedPane.addTab("Authorization", authPanelWrapper)

        // Tab: Headers
        headersTable = JBTable(headersTableModel)
        headersTable.setShowGrid(true)
        headersTableModel.addRow(arrayOf("Content-Type", "application/json")) // Default header
        val headersPanel = createTablePanel(headersTable, headersTableModel)
        tabbedPane.addTab("Headers", headersPanel)

        // Tab: Body with Content-Type selector and format button
        val bodyPanel = JPanel(BorderLayout())

        val bodyToolbar = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))

        // Content-Type selector
        bodyToolbar.add(JLabel("Content-Type:"))
        bodyTypeComboBox = ComboBox(BodyType.entries.toTypedArray())
        bodyTypeComboBox.selectedItem = selectedBodyType
        bodyTypeComboBox.addActionListener {
            selectedBodyType = bodyTypeComboBox.selectedItem as BodyType
            updateBodyEditorVisibility()
            updateContentTypeHeader()
            formatRequestBody() // Auto-format when changing type
        }
        bodyToolbar.add(bodyTypeComboBox)

        // Format button
        val formatButton = JButton("Format")
        formatButton.addActionListener { formatRequestBody() }
        bodyToolbar.add(formatButton)

        bodyPanel.add(bodyToolbar, BorderLayout.NORTH)

        // Card layout for switching between Raw, Multipart, and GraphQL editors
        val bodyCardPanel = JPanel(CardLayout())
        requestBodyArea = createTextArea()

        // Raw editor panel
        val rawEditorPanel = JBScrollPane(requestBodyArea)
        bodyCardPanel.add(rawEditorPanel, "RAW")

        // Multipart editor panel
        val multipartEditorPanel = createMultipartPanel()
        bodyCardPanel.add(multipartEditorPanel, "MULTIPART")

        // GraphQL editor panel
        graphqlPanel = GraphQLPanel()
        val graphqlEditorPanel = graphqlPanel.createPanel()
        bodyCardPanel.add(graphqlEditorPanel, "GRAPHQL")

        bodyPanel.add(bodyCardPanel, BorderLayout.CENTER)
        tabbedPane.addTab("Body", bodyPanel)

        // Initialize visibility
        updateBodyEditorVisibility()

        // Tab: Environments
        environmentPanel = EnvironmentPanel(project)
        tabbedPane.addTab("Environments", environmentPanel)

        // Tab: Collections
        collectionsPanel = CollectionsPanel(project) { requestItem ->
            // Load request from collection (including scripts)
            loadRequest(requestItem)
        }
        tabbedPane.addTab("Collections", collectionsPanel)

        // Tab: Pre-request Script
        val preRequestPanel = JPanel(BorderLayout())
        preRequestScriptArea = createTextArea()
        preRequestScriptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val preRequestInfoLabel = JLabel("// 在发送请求前执行的脚本。可使用 am.environment.set(key, value) 设置环境变量")
        preRequestInfoLabel.border = JBUI.Borders.empty(5)
        preRequestPanel.add(preRequestInfoLabel, BorderLayout.NORTH)
        preRequestPanel.add(JBScrollPane(preRequestScriptArea), BorderLayout.CENTER)
        tabbedPane.addTab("Pre-request", preRequestPanel)

        // Tab: Tests
        val testsPanel = JPanel(BorderLayout())
        testsScriptArea = createTextArea()
        testsScriptArea.font = Font("Monospaced", Font.PLAIN, 13)
        val testsInfoLabel = JLabel("// 请求完成后执行的测试脚本。可使用 pm.test(name, fn) 添加断言")
        testsInfoLabel.border = JBUI.Borders.empty(5)
        testsPanel.add(testsInfoLabel, BorderLayout.NORTH)
        testsPanel.add(JBScrollPane(testsScriptArea), BorderLayout.CENTER)
        tabbedPane.addTab("Tests", testsPanel)

        // Tab: Mock Server
        mockServerPanel = MockServerPanel(project)
        tabbedPane.addTab("Mock", mockServerPanel)

        // Response Area with tabs for Headers/Body/Raw
        val responsePanel = JPanel(BorderLayout())

        val statusPanel = JPanel(BorderLayout())
        statusLabel = JLabel(statusText)
        statusLabel.border = JBUI.Borders.empty(5)
        responseSizeLabel = JLabel("")
        responseSizeLabel.border = JBUI.Borders.empty(5)

        val copyResponseButton = JButton("Copy")
        copyResponseButton.addActionListener { copyResponse() }

        val saveResponseButton = JButton("Save")
        saveResponseButton.addActionListener { saveResponseToFile() }

        val clearResponseButton = JButton("Clear")
        clearResponseButton.addActionListener { clearResponse() }

        val responseButtonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        responseButtonPanel.add(responseSizeLabel)
        responseButtonPanel.add(copyResponseButton)
        responseButtonPanel.add(saveResponseButton)
        responseButtonPanel.add(clearResponseButton)

        statusPanel.add(statusLabel, BorderLayout.WEST)
        statusPanel.add(responseButtonPanel, BorderLayout.EAST)

        // Response tabs
        responseTabbedPane = JBTabbedPane()

        // Body tab with high-performance viewer
        responseViewer = HighPerfResponseViewer(project, this)
        responseTabbedPane.addTab("Body", responseViewer)

        // Headers tab
        responseHeadersArea = createTextArea()
        responseHeadersArea.isEditable = false
        responseTabbedPane.addTab("Headers", JBScrollPane(responseHeadersArea))

        // Raw tab
        responseRawArea = createTextArea()
        responseRawArea.isEditable = false
        responseTabbedPane.addTab("Raw", JBScrollPane(responseRawArea))

        // Test Results tab
        responseTestResultsArea = createTextArea()
        responseTestResultsArea.isEditable = false
        responseTestResultsArea.font = Font("Monospaced", Font.PLAIN, 12)
        responseTabbedPane.addTab("Test Results", JBScrollPane(responseTestResultsArea))

        // Profiling tab
        profilingPanel = ProfilingPanel()
        responseTabbedPane.addTab("Profiling", profilingPanel)

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
            when (selectedBodyType) {
                BodyType.GRAPHQL -> {
                    graphqlPanel.prettifyQuery()
                    val variables = graphqlPanel.getVariables()
                    if (variables.isNotBlank()) {
                        try {
                            val node = com.github.dhzhu.amateurpostman.services.JsonService.mapper.readTree(variables)
                            graphqlPanel.setVariables(com.github.dhzhu.amateurpostman.services.JsonService.mapper.writeValueAsString(node))
                        } catch (e: Exception) {
                            // Variables may not be valid JSON yet, skip formatting
                        }
                    }
                }
                else -> {
                    val content = requestBodyArea.text
                    if (content.isBlank()) return

                    when (selectedBodyType) {
                        BodyType.JSON -> {
                            val node = com.github.dhzhu.amateurpostman.services.JsonService.mapper.readTree(content)
                            requestBodyArea.text = com.github.dhzhu.amateurpostman.services.JsonService.mapper.writeValueAsString(node)
                        }
                        BodyType.XML -> {
                            requestBodyArea.text = formatXml(content)
                        }
                        BodyType.GRAPHQL, BodyType.HTML, BodyType.JAVASCRIPT, BodyType.TEXT, BodyType.MULTIPART, BodyType.FORM_URLENCODED -> {
                            // No formatting needed for these types (GraphQL handled above)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail - user may be typing
        }
    }

    /**
     * 格式化 XML 字符串为易读的缩进格式。
     *
     * **已知局限性**：
     * - 不支持自闭合标签 (`<tag/>`) - 可能导致缩进错误
     * - 不支持 CDATA 区块 (`<![CDATA[...]]>`) - 内容会被错误解析
     * - DOCTYPE 声明处理不完善
     * - 注释 (`<!-- ... -->`) 处理不可靠
     * - 可能存在边界越界问题（非常规 XML 结构）
     *
     * 对于复杂 XML 文档，建议使用专业库如 `org.dom4j` 或 `javax.xml.parsers`。
     * 当前实现适用于简单 XML 响应的快速美化展示。
     */
    private fun formatXml(xml: String): String {
        // Basic XML pretty-printing
        val indent = "    "
        val formatted = StringBuilder()
        var indentationLevel = 0
        var inTag = false

        for (i in xml.indices) {
            val c = xml[i]
            when {
                c == '<' -> {
                    if (xml[i + 1] == '/') {
                        // Closing tag
                        indentationLevel--
                        formatted.append('\n').append(indent.repeat(indentationLevel))
                    } else if (xml.length > i + 1 && xml[i + 1] == '?' || xml[i + 1] == '!') {
                        // Processing instruction or comment
                        formatted.append('\n').append(indent.repeat(indentationLevel))
                    } else {
                        // Opening tag
                        formatted.append('\n').append(indent.repeat(indentationLevel))
                        indentationLevel++
                    }
                    inTag = true
                    formatted.append(c)
                }
                c == '>' -> {
                    formatted.append(c)
                    inTag = false
                }
                c == '\n' || c == '\r' -> {
                    // Skip existing newlines
                }
                !inTag && !c.isWhitespace() -> {
                    formatted.append(c)
                }
                inTag -> {
                    formatted.append(c)
                }
            }
        }

        return formatted.toString().trim()
    }

    private fun updateContentTypeHeader() {
        // Update or add Content-Type header based on selected body type
        var contentTypeUpdated = false
        for (i in 0 until headersTableModel.rowCount) {
            val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
            if (key?.equals("Content-Type", ignoreCase = true) == true) {
                headersTableModel.setValueAt(selectedBodyType.mimeType, i, 1)
                contentTypeUpdated = true
                break
            }
        }

        // If no Content-Type header exists, add one
        if (!contentTypeUpdated && selectedBodyType != BodyType.TEXT && selectedBodyType != BodyType.MULTIPART) {
            headersTableModel.addRow(arrayOf("Content-Type", selectedBodyType.mimeType))
        }

        // For Multipart, remove Content-Type header (OkHttp will set it automatically)
        if (selectedBodyType == BodyType.MULTIPART) {
            for (i in headersTableModel.rowCount - 1 downTo 0) {
                val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
                if (key?.equals("Content-Type", ignoreCase = true) == true) {
                    headersTableModel.removeRow(i)
                    break
                }
            }
        }
    }

    private fun updateBodyEditorVisibility() {
        val bodyCardPanel = findBodyCardPanel()
        if (bodyCardPanel != null) {
            val layout = bodyCardPanel.layout as CardLayout
            when (selectedBodyType) {
                BodyType.MULTIPART -> layout.show(bodyCardPanel, "MULTIPART")
                BodyType.GRAPHQL -> layout.show(bodyCardPanel, "GRAPHQL")
                else -> layout.show(bodyCardPanel, "RAW")
            }
        }
    }

    private fun findBodyCardPanel(): JPanel? {
        // Helper to find the body card panel
        var current: Component? = tabbedPane.getSelectedComponent()
        if (current is JPanel) {
            for (comp in current.components) {
                if (comp is JPanel && comp.layout is CardLayout) {
                    return comp
                }
            }
        }
        return null
    }

    private fun createMultipartPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        multipartTable = JBTable(multipartTableModel)
        multipartTable.setShowGrid(true)

        // Set column widths
        multipartTable.columnModel.getColumn(0).preferredWidth = 150 // Key
        multipartTable.columnModel.getColumn(1).preferredWidth = 100 // Type
        multipartTable.columnModel.getColumn(2).preferredWidth = 250 // Value
        multipartTable.columnModel.getColumn(3).preferredWidth = 150 // Content-Type
        multipartTable.columnModel.getColumn(4).preferredWidth = 150 // Description

        // Custom editor for Type column (Text/File)
        val typeColumn = multipartTable.columnModel.getColumn(1)
        val typeComboBox = JComboBox(arrayOf("Text", "File"))
        typeColumn.cellEditor = javax.swing.DefaultCellEditor(typeComboBox)

        // Custom renderer for Value column to show file path with browse button
        multipartTable.columnModel.getColumn(2).cellEditor = MultipartCellEditor(this)

        panel.add(JBScrollPane(multipartTable), BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addTextButton = JButton("Add Text")
        addTextButton.addActionListener {
            multipartTableModel.addRow(arrayOf("", "Text", "", "", ""))
            val newRow = multipartTableModel.rowCount - 1
            multipartTable.setRowSelectionInterval(newRow, newRow)
            multipartTable.scrollRectToVisible(multipartTable.getCellRect(newRow, 0, true))
        }

        val addFileButton = JButton("Add File")
        addFileButton.addActionListener {
            multipartTableModel.addRow(arrayOf("", "File", "", "", ""))
            val newRow = multipartTableModel.rowCount - 1
            multipartTable.setRowSelectionInterval(newRow, newRow)
            multipartTable.scrollRectToVisible(multipartTable.getCellRect(newRow, 0, true))
        }

        val removeButton = JButton("Remove")
        removeButton.addActionListener {
            val selectedRow = multipartTable.selectedRow
            if (selectedRow >= 0) {
                multipartTableModel.removeRow(selectedRow)
                if (multipartTableModel.rowCount > 0) {
                    val newSelection = if (selectedRow > 0) selectedRow - 1 else 0
                    multipartTable.setRowSelectionInterval(newSelection, newSelection)
                }
            }
        }

        buttonPanel.add(addTextButton)
        buttonPanel.add(addFileButton)
        buttonPanel.add(removeButton)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    fun browseForFile(): String? {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.selectedFile.absolutePath
        }
        return null
    }

    private fun copyResponse() {
        val text = responseViewer.getText()
        if (text.isNotEmpty()) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            statusLabel.text = "Response copied to clipboard"
        }
    }

    private fun saveResponseToFile() {
        val text = responseViewer.getText()
        if (text.isEmpty()) return

        val chooser = JFileChooser()
        chooser.dialogTitle = "Save Response"
        val ext = when {
            responseViewer.currentContentType.contains("application/json", ignoreCase = true) -> "json"
            responseViewer.currentContentType.contains("xml", ignoreCase = true) -> "xml"
            responseViewer.currentContentType.contains("html", ignoreCase = true) -> "html"
            else -> "txt"
        }
        chooser.selectedFile = java.io.File("response.$ext")
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                chooser.selectedFile.writeText(text, Charsets.UTF_8)
                statusLabel.text = "Response saved to ${chooser.selectedFile.name}"
            } catch (e: Exception) {
                statusLabel.text = "Save failed: ${e.message}"
            }
        }
    }

    private fun clearResponse() {
        responseViewer.clear()
        responseHeadersArea.text = ""
        responseRawArea.text = ""
        responseTestResultsArea.text = ""
        responseSizeLabel.text = ""
        statusLabel.text = "Ready"
        profilingPanel.clear()
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

        // Set body and body type
        entry.request.body?.let { body ->
            selectedBodyType = body.type
            bodyTypeComboBox.selectedItem = body.type

            when (body.type) {
                BodyType.GRAPHQL -> {
                    // Try to parse JSON and load into GraphQL panel
                    val graphQLRequest = GraphQLRequest.fromJson(body.content)
                    if (graphQLRequest != null) {
                        graphqlPanel.loadGraphQLRequest(graphQLRequest)
                    } else {
                        // If parsing fails, set the content as query
                        graphqlPanel.setQuery(body.content)
                    }
                }
                else -> {
                    requestBodyArea.text = body.content
                }
            }
        } ?: run {
            requestBodyArea.text = ""
        }

        statusLabel.text = "Loaded from history: ${entry.getDisplayName()}"
    }

    /**
     * Loads a request into the form fields.
     * Used when loading from collections.
     */
    private fun loadRequest(requestItem: com.github.dhzhu.amateurpostman.models.CollectionItem.Request) {
        currentEditingRequestItem = requestItem

        urlField.text = requestItem.request.url
        methodComboBox.selectedItem = requestItem.request.method
        selectedMethod = requestItem.request.method

        // Clear and set headers
        while (headersTableModel.rowCount > 0) {
            headersTableModel.removeRow(0)
        }
        requestItem.request.headers.forEach { (key, value) ->
            headersTableModel.addRow(arrayOf(key, value))
        }

        // Set body and body type
        requestItem.request.body?.let { body ->
            selectedBodyType = body.type
            bodyTypeComboBox.selectedItem = body.type

            when (body.type) {
                BodyType.GRAPHQL -> {
                    // Try to parse JSON and load into GraphQL panel
                    val graphQLRequest = GraphQLRequest.fromJson(body.content)
                    if (graphQLRequest != null) {
                        graphqlPanel.loadGraphQLRequest(graphQLRequest)
                    } else {
                        // If parsing fails, set the content as query
                        graphqlPanel.setQuery(body.content)
                    }
                }
                else -> {
                    requestBodyArea.text = body.content
                }
            }
        } ?: run {
            requestBodyArea.text = ""
        }

        // Load scripts
        preRequestScriptArea.text = requestItem.preRequestScript
        testsScriptArea.text = requestItem.testScript

        statusLabel.text = "Loaded request: ${requestItem.name}"
    }

    /**
     * Loads a request into the form fields (without scripts).
     * Used when loading from history or other sources.
     */
    private fun loadRequest(request: HttpRequest) {
        urlField.text = request.url
        methodComboBox.selectedItem = request.method
        selectedMethod = request.method

        // Clear and set headers
        while (headersTableModel.rowCount > 0) {
            headersTableModel.removeRow(0)
        }
        request.headers.forEach { (key, value) ->
            headersTableModel.addRow(arrayOf(key, value))
        }

        // Set body and body type
        request.body?.let { body ->
            selectedBodyType = body.type
            bodyTypeComboBox.selectedItem = body.type

            when (body.type) {
                BodyType.GRAPHQL -> {
                    // Try to parse JSON and load into GraphQL panel
                    val graphQLRequest = GraphQLRequest.fromJson(body.content)
                    if (graphQLRequest != null) {
                        graphqlPanel.loadGraphQLRequest(graphQLRequest)
                    } else {
                        // If parsing fails, set the content as query
                        graphqlPanel.setQuery(body.content)
                    }
                }
                else -> {
                    requestBodyArea.text = body.content
                }
            }
        } ?: run {
            requestBodyArea.text = ""
        }

        statusLabel.text = "Loaded request"
    }

    /**
     * Opens the Save Request dialog to save the current request.
     */
    private fun saveRequest() {
        val request = buildRequest()
        if (request == null) {
            statusLabel.text = "Error: Unable to build request"
            return
        }

        // If editing an existing request from collection, update it
        if (currentEditingRequestItem != null) {
            // Find which collection this request belongs to
            val collectionService = project.service<com.github.dhzhu.amateurpostman.services.CollectionService>()
            for (collection in collectionService.getCollections()) {
                if (collection.findItemById(currentEditingRequestItem!!.id) != null) {
                    // Update the existing request
                    val preRequestScript = preRequestScriptArea.text
                    val testScript = testsScriptArea.text
                    collectionService.updateRequest(
                        collection.id,
                        currentEditingRequestItem!!.id,
                        request,
                        preRequestScript,
                        testScript
                    )
                    statusLabel.text = "Updated request: ${currentEditingRequestItem!!.name}"
                    return
                }
            }
        }

        // Otherwise, save as new request
        SaveRequestDialog.show(project, request) { collectionId, folderId, name, description ->
            val collectionService = project.service<com.github.dhzhu.amateurpostman.services.CollectionService>()

            // Include scripts when saving
            val preRequestScript = preRequestScriptArea.text
            val testScript = testsScriptArea.text
            collectionService.addRequest(
                collectionId,
                request,
                name,
                description,
                preRequestScript,
                testScript,
                folderId
            )

            SwingUtilities.invokeLater {
                statusLabel.text = "Saved request: $name"
            }
        }
    }

    /**
     * Builds an HttpRequest from the current form fields.
     * Returns null if the request cannot be built.
     */
    private fun buildRequest(): HttpRequest? {
        // Validate URL is not empty
        val urlText = urlField.text.trim()
        if (urlText.isEmpty()) {
            return null
        }

        // Collect headers
        val headers = mutableMapOf<String, String>()
        for (i in 0 until headersTableModel.rowCount) {
            val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
            val value = headersTableModel.getValueAt(i, 1)?.toString()?.trim()
            if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                headers[key] = value
            }
        }

        // Handle Auth - get from AuthPanel
        val authentication = authPanelWrapper.getAuthentication()

        // Handle Params (Append to URL)
        var url = urlText
        val params = mutableListOf<String>()

        // Handle API Key query params
        if (authentication is ApiKeyAuth && authentication.addTo == ApiKeyAuth.ApiKeyLocation.QUERY) {
            params.add("${authentication.key}=${authentication.value}")
        }

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

        // Build request body
        val requestBody = when (selectedBodyType) {
            BodyType.MULTIPART -> {
                val parts = buildMultipartParts()
                if (parts.isNotEmpty()) {
                    HttpBody(
                        content = "",
                        type = BodyType.MULTIPART,
                        multipartData = parts
                    )
                } else null
            }
            BodyType.GRAPHQL -> {
                if (!graphqlPanel.isEmpty()) {
                    HttpBody(
                        content = graphqlPanel.toJson(),
                        type = BodyType.GRAPHQL
                    )
                } else null
            }
            else -> {
                if (requestBodyArea.text.isNotBlank()) {
                    HttpBody(
                        content = requestBodyArea.text,
                        type = selectedBodyType
                    )
                } else null
            }
        }

        // Build request
        return HttpRequest(
            url = url,
            method = selectedMethod,
            headers = headers,
            body = requestBody,
            authentication = authentication
        )
    }

    private fun buildMultipartParts(): List<MultipartPart> {
        val parts = mutableListOf<MultipartPart>()

        for (i in 0 until multipartTableModel.rowCount) {
            val key = multipartTableModel.getValueAt(i, 0)?.toString()?.trim() ?: continue
            val type = multipartTableModel.getValueAt(i, 1)?.toString()?.trim() ?: continue
            val value = multipartTableModel.getValueAt(i, 2)?.toString()?.trim() ?: ""
            val contentType = multipartTableModel.getValueAt(i, 3)?.toString()?.trim()
            val description = multipartTableModel.getValueAt(i, 4)?.toString()?.trim() ?: ""

            if (key.isEmpty()) continue

            when (type) {
                "Text" -> {
                    parts.add(MultipartPart.TextField(
                        key = key,
                        value = value,
                        contentType = contentType,
                        description = description
                    ))
                }
                "File" -> {
                    if (value.isNotEmpty()) {
                        val fileName = value.substringAfterLast("/")
                        parts.add(MultipartPart.FileField(
                            key = key,
                            filePath = value,
                            fileName = fileName,
                            contentType = contentType,
                            description = description
                        ))
                    }
                }
            }
        }

        return parts
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

            // Set body and body type
            request.body?.let { body ->
                requestBodyArea.text = body.content
                selectedBodyType = body.type
                bodyTypeComboBox.selectedItem = body.type
            } ?: run {
                requestBodyArea.text = ""
            }

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

        // Get authentication from AuthPanel
        val authentication = authPanelWrapper.getAuthentication()

        val request =
                HttpRequest(
                        url = urlField.text.trim(),
                        method = selectedMethod,
                        headers = headers,
                        body = if (requestBodyArea.text.isNotBlank()) {
                            HttpBody(
                                content = requestBodyArea.text,
                                type = selectedBodyType
                            )
                        } else null,
                        authentication = authentication
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
        responseViewer.clear()
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

                        // Get authentication from AuthPanel
                        val authentication = authPanelWrapper.getAuthentication()

                        // Handle API Key query params
                        val apiQueryParam = if (authentication is ApiKeyAuth && authentication.addTo == ApiKeyAuth.ApiKeyLocation.QUERY) {
                            "${authentication.key}=${authentication.value}"
                        } else null

                        // Handle Params (Append to URL)
                        var url = urlText
                        val params = mutableListOf<String>()
                        if (apiQueryParam != null) {
                            params.add(apiQueryParam)
                        }
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

                        // Build request
                        val requestBody = when (selectedBodyType) {
                            BodyType.MULTIPART -> {
                                val parts = buildMultipartParts()
                                if (parts.isNotEmpty()) {
                                    HttpBody(
                                        content = "",
                                        type = BodyType.MULTIPART,
                                        multipartData = parts
                                    )
                                } else null
                            }
                            else -> {
                                if (requestBodyArea.text.isNotBlank()) {
                                    HttpBody(
                                        content = requestBodyArea.text,
                                        type = selectedBodyType
                                    )
                                } else null
                            }
                        }

                        val request =
                                HttpRequest(
                                        url = url,
                                        method = selectedMethod,
                                        headers = headers,
                                        body = requestBody,
                                        authentication = authentication
                                )

                        // Execute Pre-request script and send request
                        val response = if (preRequestScriptArea.text.isNotBlank()) {
                            val scriptVars = scriptExecutionService.executePreRequestScript(preRequestScriptArea.text)
                            // Re-resolve variables in request with new values
                            val environmentService = project.service<com.github.dhzhu.amateurpostman.services.EnvironmentService>()
                            val allVars = environmentService.getCurrentEnvironmentVariables() + scriptVars
                            val resolvedRequest = com.github.dhzhu.amateurpostman.utils.VariableResolver.substitute(
                                request,
                                allVars
                            )
                            httpService.executeRequest(resolvedRequest)
                        } else {
                            httpService.executeRequest(request)
                        }

                        // Display
                        displayResponse(response)

                        // Save to history
                        historyService.addEntry(request, response)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Request was cancelled, already handled
                    } catch (e: Exception) {
                        statusLabel.text = "Error: ${e.message}"
                        responseViewer.setResponseBody(
                                "Error executing request:\n${e.message}\n\n${e.stackTraceToString()}",
                                "text/plain"
                        )
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

        // Populate Body tab with high-performance viewer
        val body = formatResponseBody(response.body, response.headers)
        val contentType = response.headers["Content-Type"]?.firstOrNull() ?: ""
        responseViewer.setResponseBody(body, contentType)

        // Execute Tests script
        scope.launch {
            val testScript = testsScriptArea.text
            if (testScript.isNotBlank()) {
                val testResult = scriptExecutionService.executeTestScript(testScript, response)
                displayTestResults(testResult)
            } else {
                responseTestResultsArea.text = "No tests defined."
            }
        }

        // Update Profiling panel
        profilingPanel.updateProfilingData(response.profilingData)
    }

    /**
     * Displays test results in the Test Results tab.
     */
    private fun displayTestResults(result: com.github.dhzhu.amateurpostman.services.TestResult) {
        val resultsText = buildString {
            appendLine("=".repeat(60))
            appendLine("Test Results: ${if (result.passed) "PASSED" else "FAILED"}")
            appendLine("=".repeat(60))
            appendLine(result.getSummary())
            appendLine()
            result.results.forEach { assertion ->
                val status = if (assertion.passed) "✓ PASS" else "✗ FAIL"
                val color = if (assertion.passed) "GREEN" else "RED"
                appendLine("$status: ${assertion.name}")
                if (assertion.message.isNotEmpty()) {
                    appendLine("    ${assertion.message}")
                }
                appendLine()
            }
        }
        responseTestResultsArea.text = resultsText
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
            val node = com.github.dhzhu.amateurpostman.services.JsonService.mapper.readTree(json)
            com.github.dhzhu.amateurpostman.services.JsonService.mapper.writeValueAsString(node)
        } catch (e: Exception) {
            json
        }
    }

    /**
     * Gets the currently active collection ID, if any.
     * This is determined based on the currently edited request or other context.
     *
     * @return The current collection ID or null if none is active
     */
    private fun getCurrentCollectionId(): String? {
        // If we're currently editing a request item, return its collection ID
        currentEditingRequestItem?.let { requestItem ->
            // Find which collection this request belongs to
            val collectionService = project.service<com.github.dhzhu.amateurpostman.services.CollectionService>()
            for (collection in collectionService.getCollections()) {
                if (collection.findItemById(requestItem.id) != null) {
                    return collection.id
                }
            }
        }

        // Otherwise, return the last known active collection ID
        return currentActiveCollectionId
    }

    /**
     * Sets the current active collection ID.
     * This can be called when a collection is selected in the UI.
     *
     * @param collectionId The collection ID to set as active
     */
    fun setCurrentActiveCollection(collectionId: String?) {
        currentActiveCollectionId = collectionId
    }

    /**
     * Loads a request from an external source (e.g., controller gutter icon).
     * This is called when clicking the gutter icon on a controller method.
     *
     * @param request The HTTP request to load
     * @param name Optional name for the request
     */
    fun loadExternalRequest(request: HttpRequest, name: String? = null) {
        SwingUtilities.invokeLater {
            loadRequest(request)
            statusLabel.text = if (name != null) "Loaded: $name" else "Loaded from controller"
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

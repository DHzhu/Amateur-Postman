package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.services.grpc.GrpcCallRequest
import com.github.dhzhu.amateurpostman.services.grpc.GrpcCallResult
import com.github.dhzhu.amateurpostman.services.grpc.GrpcMethodInfo
import com.github.dhzhu.amateurpostman.services.grpc.GrpcMethodType
import com.github.dhzhu.amateurpostman.services.grpc.GrpcRequestService
import com.github.dhzhu.amateurpostman.services.grpc.GrpcServiceInfo
import com.github.dhzhu.amateurpostman.services.grpc.GrpcStreamMessage
import com.github.dhzhu.amateurpostman.services.grpc.GrpcStreamRequest
import com.github.dhzhu.amateurpostman.services.grpc.GrpcStreamState
import com.github.dhzhu.amateurpostman.services.grpc.GrpcStreamingService
import com.github.dhzhu.amateurpostman.services.grpc.ProtoParseResult
import com.github.dhzhu.amateurpostman.services.grpc.ProtoParser
import com.github.dhzhu.amateurpostman.utils.VariableResolver
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.io.File
import javax.swing.BorderFactory
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel
import com.intellij.ui.table.JBTable
import com.intellij.openapi.ui.ComboBox

/**
 * GrpcEditorPanel — the complete gRPC request editor UI.
 *
 * Features:
 * - High-performance message list with JSON syntax highlighting
 * - Disposable pattern for proper resource cleanup
 * - Support for all gRPC streaming types
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────┐
 *   │  [Proto File: ____________] [Browse] [Load]     │
 *   │  Service: [▼ GreeterService]  Method: [▼ Say…] │
 *   │  Host: [_________] Port: [_____] [Send gRPC]   │
 *   ├──────────────────────┬──────────────────────────┤
 *   │  Request             │ Response                 │
 *   │  [Body tab]          │ [Body / Status / Meta]   │
 *   │  [Metadata tab]      │                          │
 *   └──────────────────────┴──────────────────────────┘
 */
class GrpcEditorPanel(private val project: Project) : Disposable {

    private val parser = ProtoParser()
    private val grpcService = GrpcRequestService(parser)
    private val streamingService = GrpcStreamingService(parser)
    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    // ─── State ────────────────────────────────────────────────────────────────
    private var lastProtoFile: File? = null
    private var parseResult: ProtoParseResult? = null
    private var currentServiceDescriptorMap: Map<String, com.google.protobuf.Descriptors.ServiceDescriptor> = emptyMap()
    private var fileDescriptors: Map<String, com.google.protobuf.Descriptors.FileDescriptor> = emptyMap()
    private var currentMethodType: GrpcMethodType = GrpcMethodType.UNARY

    // ─── UI components ────────────────────────────────────────────────────────
    private lateinit var protoPathField: JBTextField
    private lateinit var serviceComboBox: ComboBox<String>
    private lateinit var methodComboBox: ComboBox<String>
    private lateinit var hostField: JBTextField
    private lateinit var portField: JBTextField
    private lateinit var useTlsCheckBox: javax.swing.JCheckBox
    private lateinit var sendButton: JButton
    private lateinit var completeButton: JButton
    private lateinit var cancelButton: JButton
    private lateinit var statusLabel: JLabel

    // Streaming status
    private lateinit var streamStateLabel: JLabel
    private lateinit var streamCountLabel: JLabel

    // Request panels
    private lateinit var bodyArea: JBTextArea
    private lateinit var metadataTableModel: DefaultTableModel
    private lateinit var metadataTable: JBTable

    // Response panels
    private lateinit var responseBodyArea: JBTextArea
    private lateinit var responseStatusLabel: JLabel
    private lateinit var responseMetaArea: JBTextArea

    // High-performance streaming message list with JSON syntax highlighting
    private lateinit var messageList: StreamMessageList<GrpcStreamMessage>

    // ─── Public factory ────────────────────────────────────────────────────────

    fun createPanel(): JPanel {
        val root = JPanel(BorderLayout(0, 4))
        root.border = JBUI.Borders.empty(6)

        root.add(createConfigBar(), BorderLayout.NORTH)
        root.add(createSplitEditor(), BorderLayout.CENTER)
        root.add(createStatusBar(), BorderLayout.SOUTH)

        setupStreamObservation()

        return root
    }

    // ─── Config bar (Proto path + Service/Method + Host/Port) ─────────────────

    private fun createConfigBar(): JPanel {
        val panel = JPanel(BorderLayout(0, 4))
        panel.border = JBUI.Borders.emptyBottom(4)

        // Row 1: Proto file
        val protoRow = JPanel(BorderLayout(4, 0))
        protoRow.add(JBLabel("Proto:"), BorderLayout.WEST)
        protoPathField = JBTextField()
        protoPathField.toolTipText = "Path to .proto file"
        protoRow.add(protoPathField, BorderLayout.CENTER)

        val browseButton = JButton("Browse…")
        browseButton.addActionListener { browseProtoFile() }
        val loadButton = JButton("Load")
        loadButton.addActionListener { loadProtoFile() }

        val protoButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        protoButtonPanel.add(browseButton)
        protoButtonPanel.add(loadButton)
        protoRow.add(protoButtonPanel, BorderLayout.EAST)

        // Row 2: Service + Method + Host + Port + TLS + Send
        val callRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))

        callRow.add(JBLabel("Service:"))
        serviceComboBox = ComboBox(DefaultComboBoxModel())
        serviceComboBox.preferredSize = Dimension(180, serviceComboBox.preferredSize.height)
        serviceComboBox.toolTipText = "Select gRPC Service"
        serviceComboBox.addActionListener { onServiceChanged() }
        callRow.add(serviceComboBox)

        callRow.add(JBLabel("Method:"))
        methodComboBox = ComboBox(DefaultComboBoxModel())
        methodComboBox.preferredSize = Dimension(180, methodComboBox.preferredSize.height)
        methodComboBox.toolTipText = "Select gRPC Method"
        methodComboBox.addActionListener { onMethodChanged() }
        callRow.add(methodComboBox)

        callRow.add(JBLabel("Host:"))
        hostField = JBTextField("localhost")
        hostField.preferredSize = Dimension(140, hostField.preferredSize.height)
        callRow.add(hostField)

        callRow.add(JBLabel("Port:"))
        portField = JBTextField("50051")
        portField.preferredSize = Dimension(70, portField.preferredSize.height)
        callRow.add(portField)

        useTlsCheckBox = javax.swing.JCheckBox("TLS")
        callRow.add(useTlsCheckBox)

        sendButton = JButton("▶ Send")
        sendButton.isEnabled = false
        sendButton.addActionListener { sendGrpcRequest() }
        callRow.add(sendButton)

        completeButton = JButton("✓ Complete")
        completeButton.isEnabled = false
        completeButton.toolTipText = "Complete client stream (Client/Bidi streaming only)"
        completeButton.addActionListener { completeStream() }
        callRow.add(completeButton)

        cancelButton = JButton("✗ Cancel")
        cancelButton.isEnabled = false
        cancelButton.toolTipText = "Cancel active stream"
        cancelButton.addActionListener { cancelStream() }
        callRow.add(cancelButton)

        panel.add(protoRow, BorderLayout.NORTH)
        panel.add(callRow, BorderLayout.SOUTH)

        return panel
    }

    // ─── Split editor (Request | Response) ────────────────────────────────────

    private fun createSplitEditor(): JSplitPane {
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createRequestPanel(), createResponsePanel())
        split.resizeWeight = 0.5
        split.isContinuousLayout = true
        return split
    }

    private fun createRequestPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Request")

        val tabs = JBTabbedPane()

        // Body tab (Step 3.4)
        val bodyPanel = JPanel(BorderLayout())
        val bodyToolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        val formatButton = JButton("Format JSON")
        formatButton.addActionListener { formatBodyJson() }
        val templateButton = JButton("Generate Template")
        templateButton.toolTipText = "Fill body with default values from selected method's input type"
        templateButton.addActionListener { generateBodyTemplate() }
        bodyToolbar.add(formatButton)
        bodyToolbar.add(templateButton)
        bodyPanel.add(bodyToolbar, BorderLayout.NORTH)

        bodyArea = JBTextArea()
        bodyArea.font = Font("Monospaced", Font.PLAIN, 13)
        bodyArea.text = "{\n  \n}"
        bodyPanel.add(JBScrollPane(bodyArea), BorderLayout.CENTER)
        tabs.addTab("Body", bodyPanel)

        // Metadata tab (gRPC headers)
        val metaPanel = JPanel(BorderLayout())
        metadataTableModel = DefaultTableModel(arrayOf("Key", "Value"), 0)
        metadataTable = JBTable(metadataTableModel)
        metadataTable.setShowGrid(true)
        metaPanel.add(JBScrollPane(metadataTable), BorderLayout.CENTER)

        val metaButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addMetaRow = JButton("Add")
        addMetaRow.addActionListener { metadataTableModel.addRow(arrayOf("", "")) }
        val removeMetaRow = JButton("Remove")
        removeMetaRow.addActionListener {
            val sel = metadataTable.selectedRow
            if (sel >= 0) metadataTableModel.removeRow(sel)
        }
        metaButtonPanel.add(addMetaRow)
        metaButtonPanel.add(removeMetaRow)
        metaPanel.add(metaButtonPanel, BorderLayout.SOUTH)
        tabs.addTab("Metadata", metaPanel)

        panel.add(tabs, BorderLayout.CENTER)
        return panel
    }

    private fun createResponsePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Response")

        // Streaming status line
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 2))
        streamStateLabel = JLabel("IDLE")
        streamStateLabel.icon = StreamStateIcon(GrpcStreamState.IDLE)
        statusPanel.add(streamStateLabel)
        statusPanel.add(JLabel("|"))
        streamCountLabel = JLabel("Sent: 0 | Received: 0")
        statusPanel.add(streamCountLabel)
        panel.add(statusPanel, BorderLayout.NORTH)

        // Tabs for Messages + Body + Trailing Metadata
        val tabs = JBTabbedPane()

        // Messages tab (for streaming) - using high-performance component
        messageList = StreamMessageList(maxMessages = 1000)
        tabs.addTab("Messages", messageList)

        // Body tab (for unary response)
        responseBodyArea = JBTextArea()
        responseBodyArea.font = Font("Monospaced", Font.PLAIN, 13)
        responseBodyArea.isEditable = false
        tabs.addTab("Body", JBScrollPane(responseBodyArea))

        responseMetaArea = JBTextArea()
        responseMetaArea.font = Font("Monospaced", Font.PLAIN, 12)
        responseMetaArea.isEditable = false
        tabs.addTab("Trailing Metadata", JBScrollPane(responseMetaArea))

        panel.add(tabs, BorderLayout.CENTER)

        // Response status label at bottom
        responseStatusLabel = JLabel("—")
        responseStatusLabel.border = JBUI.Borders.empty(4, 6)
        panel.add(responseStatusLabel, BorderLayout.SOUTH)

        return panel
    }

    private fun createStatusBar(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2))
        statusLabel = JLabel("Load a .proto file to get started")
        panel.add(statusLabel)
        return panel
    }

    // ─── Event handlers ────────────────────────────────────────────────────────

    /** Step 3.2: Browse for proto file using IntelliJ file chooser */
    private fun browseProtoFile() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("proto")
            .withTitle("Select .proto File")
            .withDescription("Choose a Protocol Buffers schema file")
        val chosen = FileChooser.chooseFile(descriptor, project, null)
        if (chosen != null) {
            protoPathField.text = chosen.path
            loadProtoFile()
        }
    }

    /** Step 3.2 / 1.1: Invoke ProtoParser and populate service dropdown */
    private fun loadProtoFile() {
        val path = protoPathField.text.trim()
        if (path.isEmpty()) {
            statusLabel.text = "Please enter or browse to a .proto file path."
            return
        }
        val file = File(path)
        if (!file.exists()) {
            statusLabel.text = "File not found: $path"
            return
        }

        statusLabel.text = "Parsing ${file.name}…"
        sendButton.isEnabled = false

        scope.launch(Dispatchers.IO) {
            val result = parser.parse(file)

            // If parse succeeded, also load the raw FileDescriptors for real calls
            val newFileDescriptors = if (result.isSuccess) {
                try { loadFileDescriptorsFromProto(file) } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            SwingUtilities.invokeLater {
                parseResult = result
                fileDescriptors = newFileDescriptors
                lastProtoFile = file

                if (result.isSuccess) {
                    populateServiceComboBox(result.services)
                    statusLabel.text = "Loaded: ${result.services.size} service(s) from ${file.name}"
                } else {
                    statusLabel.text = "Parse error: ${result.error}"
                    clearServiceComboBoxes()
                }
            }
        }
    }

    /** Step 3.3: Populate the service dropdown and trigger method refresh */
    private fun populateServiceComboBox(services: List<GrpcServiceInfo>) {
        val model = serviceComboBox.model as DefaultComboBoxModel<String>
        model.removeAllElements()
        services.forEach { model.addElement(it.name) }
        if (services.isNotEmpty()) {
            serviceComboBox.selectedIndex = 0
            onServiceChanged()
        }
    }

    /** Step 3.3: Refresh method dropdown when service is changed */
    private fun onServiceChanged() {
        val selectedService = serviceComboBox.selectedItem as? String ?: return
        val services = parseResult?.services ?: return

        val serviceInfo = services.find { it.name == selectedService } ?: return
        val model = methodComboBox.model as DefaultComboBoxModel<String>
        model.removeAllElements()

        // Show all methods with type label
        serviceInfo.methods.forEach { method ->
            val typeLabel = when {
                method.isUnary -> "Unary"
                method.isServerStreaming -> "ServerStream"
                method.isClientStreaming -> "ClientStream"
                method.isBidirectional -> "BidiStream"
                else -> "Unknown"
            }
            model.addElement("${method.name} ($typeLabel)")
        }
        if (model.size > 0) {
            methodComboBox.selectedIndex = 0
            onMethodChanged()
        }
        sendButton.isEnabled = model.size > 0
    }

    /** Step 3.4: When method changes, update method type */
    private fun onMethodChanged() {
        val selectedMethodDisplay = methodComboBox.selectedItem as? String ?: return
        val methodName = selectedMethodDisplay.substringBefore(" (").trim()

        val selectedService = serviceComboBox.selectedItem as? String ?: return
        val services = parseResult?.services ?: return
        val serviceInfo = services.find { it.name == selectedService } ?: return
        val methodInfo = serviceInfo.methods.find { it.name == methodName } ?: return

        currentMethodType = when {
            methodInfo.isUnary -> GrpcMethodType.UNARY
            methodInfo.isServerStreaming -> GrpcMethodType.SERVER_STREAMING
            methodInfo.isClientStreaming -> GrpcMethodType.CLIENT_STREAMING
            methodInfo.isBidirectional -> GrpcMethodType.BIDI_STREAMING
            else -> GrpcMethodType.UNARY
        }

        // Update button text based on method type
        sendButton.text = when (currentMethodType) {
            GrpcMethodType.UNARY -> "▶ Send"
            GrpcMethodType.SERVER_STREAMING -> "▶ Start Stream"
            GrpcMethodType.CLIENT_STREAMING -> "▶ Start & Send"
            GrpcMethodType.BIDI_STREAMING -> "▶ Start & Send"
        }
    }

    /** Step 3.4: Generate a JSON template based on the selected method's input type */
    private fun generateBodyTemplate() {
        val selectedService = serviceComboBox.selectedItem as? String ?: return
        val selectedMethodDisplay = methodComboBox.selectedItem as? String ?: return
        val selectedMethod = selectedMethodDisplay.substringBefore(" (").trim()

        val fileDesc = fileDescriptors.values
            .firstOrNull { it.findServiceByName(selectedService) != null } ?: return
        val serviceDesc = fileDesc.findServiceByName(selectedService) ?: return
        val methodDesc = serviceDesc.findMethodByName(selectedMethod) ?: return

        try {
            val template = parser.generateJsonTemplate(methodDesc.inputType)
            bodyArea.text = template
            statusLabel.text = "Template generated for ${methodDesc.inputType.name}"
        } catch (e: Exception) {
            statusLabel.text = "Failed to generate template: ${e.message}"
        }
    }

    private fun formatBodyJson() {
        val text = bodyArea.text.trim()
        if (text.isEmpty()) return
        try {
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val element = com.google.gson.JsonParser.parseString(text)
            bodyArea.text = gson.toJson(element)
        } catch (_: Exception) {
            statusLabel.text = "Invalid JSON — cannot format"
        }
    }

    // ─── Step 3.6: Send gRPC request with variable resolution ─────────────────

    private fun sendGrpcRequest() {
        val selectedServiceName = serviceComboBox.selectedItem as? String ?: return
        val selectedMethodDisplay = methodComboBox.selectedItem as? String ?: return
        val selectedMethodName = selectedMethodDisplay.substringBefore(" (").trim()
        val host = hostField.text.trim()
        val port = portField.text.trim().toIntOrNull()

        if (host.isEmpty() || port == null) {
            statusLabel.text = "Please enter a valid host and port."
            return
        }

        // Collect metadata from table
        val rawMetadata = buildMetadataMap()

        // Collect raw body JSON
        val rawBodyJson = bodyArea.text.trim()

        // Resolve variables (Step 3.6)
        val environmentService = try {
            project.getService(com.github.dhzhu.amateurpostman.services.EnvironmentService::class.java)
        } catch (_: Exception) { null }
        val variables = environmentService?.getAllVariables() ?: emptyMap()

        val bodyJson = VariableResolver.substituteVariables(rawBodyJson, variables)
        val metadata = rawMetadata.mapValues { (_, v) ->
            VariableResolver.substituteVariables(v, variables)
        }

        // Find the actual ServiceDescriptor
        val fileDesc = fileDescriptors.values.firstOrNull {
            it.findServiceByName(selectedServiceName) != null
        }
        val serviceDesc = fileDesc?.findServiceByName(selectedServiceName)
        val methodDesc = serviceDesc?.findMethodByName(selectedMethodName)

        if (serviceDesc == null || methodDesc == null) {
            statusLabel.text = "Cannot find service/method descriptor. Please reload the .proto file."
            return
        }

        when (currentMethodType) {
            GrpcMethodType.UNARY -> sendUnaryRequest(serviceDesc, methodDesc, host, port, bodyJson, metadata, selectedServiceName, selectedMethodName)
            GrpcMethodType.SERVER_STREAMING -> startServerStreaming(serviceDesc, methodDesc, host, port, bodyJson, metadata, selectedServiceName, selectedMethodName)
            GrpcMethodType.CLIENT_STREAMING -> startClientStreaming(serviceDesc, methodDesc, host, port, bodyJson, metadata, selectedServiceName, selectedMethodName)
            GrpcMethodType.BIDI_STREAMING -> startBidiStreaming(serviceDesc, methodDesc, host, port, bodyJson, metadata, selectedServiceName, selectedMethodName)
        }
    }

    private fun sendUnaryRequest(
        serviceDesc: com.google.protobuf.Descriptors.ServiceDescriptor,
        methodDesc: com.google.protobuf.Descriptors.MethodDescriptor,
        host: String,
        port: Int,
        bodyJson: String,
        metadata: Map<String, String>,
        serviceName: String,
        methodName: String
    ) {
        val request = GrpcCallRequest(
            host = host,
            port = port,
            serviceName = serviceName,
            methodName = methodName,
            requestBodyJson = bodyJson,
            metadata = metadata,
            useTls = useTlsCheckBox.isSelected,
            deadlineSeconds = 30L
        )

        sendButton.isEnabled = false
        statusLabel.text = "Sending gRPC request…"
        responseStatusLabel.text = "—"
        responseBodyArea.text = ""
        responseMetaArea.text = ""

        scope.launch(Dispatchers.IO) {
            val result = grpcService.call(serviceDesc, request)
            SwingUtilities.invokeLater {
                sendButton.isEnabled = true
                displayResult(result, serviceName, methodName)
            }
        }
    }

    private fun startServerStreaming(
        serviceDesc: com.google.protobuf.Descriptors.ServiceDescriptor,
        methodDesc: com.google.protobuf.Descriptors.MethodDescriptor,
        host: String,
        port: Int,
        bodyJson: String,
        metadata: Map<String, String>,
        serviceName: String,
        methodName: String
    ) {
        val request = GrpcStreamRequest(
            host = host,
            port = port,
            serviceName = serviceName,
            methodName = methodName,
            metadata = metadata,
            useTls = useTlsCheckBox.isSelected
        )

        messageList.clearMessages()
        streamingService.clearHistory()

        val started = streamingService.startServerStreaming(serviceDesc, methodDesc, request, bodyJson)
        if (started) {
            statusLabel.text = "Server streaming started…"
            updateStreamingButtons(true)
        } else {
            statusLabel.text = "Failed to start server stream"
        }
    }

    private fun startClientStreaming(
        serviceDesc: com.google.protobuf.Descriptors.ServiceDescriptor,
        methodDesc: com.google.protobuf.Descriptors.MethodDescriptor,
        host: String,
        port: Int,
        bodyJson: String,
        metadata: Map<String, String>,
        serviceName: String,
        methodName: String
    ) {
        val request = GrpcStreamRequest(
            host = host,
            port = port,
            serviceName = serviceName,
            methodName = methodName,
            metadata = metadata,
            useTls = useTlsCheckBox.isSelected
        )

        messageList.clearMessages()
        streamingService.clearHistory()

        val started = streamingService.startClientStreaming(serviceDesc, methodDesc, request)
        if (started) {
            // Send first message
            if (bodyJson.isNotEmpty() && bodyJson != "{\n  \n}") {
                streamingService.sendMessage(methodDesc, bodyJson)
            }
            statusLabel.text = "Client streaming started"
            updateStreamingButtons(true)
        } else {
            statusLabel.text = "Failed to start client stream"
        }
    }

    private fun startBidiStreaming(
        serviceDesc: com.google.protobuf.Descriptors.ServiceDescriptor,
        methodDesc: com.google.protobuf.Descriptors.MethodDescriptor,
        host: String,
        port: Int,
        bodyJson: String,
        metadata: Map<String, String>,
        serviceName: String,
        methodName: String
    ) {
        val request = GrpcStreamRequest(
            host = host,
            port = port,
            serviceName = serviceName,
            methodName = methodName,
            metadata = metadata,
            useTls = useTlsCheckBox.isSelected
        )

        messageList.clearMessages()
        streamingService.clearHistory()

        val started = streamingService.startBidiStreaming(serviceDesc, methodDesc, request)
        if (started) {
            // Send first message
            if (bodyJson.isNotEmpty() && bodyJson != "{\n  \n}") {
                streamingService.sendMessage(methodDesc, bodyJson)
            }
            statusLabel.text = "Bidirectional streaming started"
            updateStreamingButtons(true)
        } else {
            statusLabel.text = "Failed to start bidi stream"
        }
    }

    private fun sendStreamMessage() {
        val selectedServiceName = serviceComboBox.selectedItem as? String ?: return
        val selectedMethodDisplay = methodComboBox.selectedItem as? String ?: return
        val selectedMethodName = selectedMethodDisplay.substringBefore(" (").trim()

        val fileDesc = fileDescriptors.values.firstOrNull {
            it.findServiceByName(selectedServiceName) != null
        }
        val serviceDesc = fileDesc?.findServiceByName(selectedServiceName)
        val methodDesc = serviceDesc?.findMethodByName(selectedMethodName) ?: return

        val rawBodyJson = bodyArea.text.trim()
        if (rawBodyJson.isEmpty() || rawBodyJson == "{\n  \n}") {
            statusLabel.text = "Please enter a message to send"
            return
        }

        val sent = streamingService.sendMessage(methodDesc, rawBodyJson)
        if (sent) {
            statusLabel.text = "Message sent"
            updateStreamCounts()
        } else {
            statusLabel.text = "Failed to send message"
        }
    }

    private fun completeStream() {
        streamingService.completeStream()
        statusLabel.text = "Stream completed"
        updateStreamingButtons(false)
    }

    private fun cancelStream() {
        streamingService.cancelStream()
        statusLabel.text = "Stream cancelled"
        updateStreamingButtons(false)
    }

    private fun updateStreamingButtons(isStreaming: Boolean) {
        sendButton.isEnabled = !isStreaming || currentMethodType == GrpcMethodType.CLIENT_STREAMING || currentMethodType == GrpcMethodType.BIDI_STREAMING
        completeButton.isEnabled = isStreaming && (currentMethodType == GrpcMethodType.CLIENT_STREAMING || currentMethodType == GrpcMethodType.BIDI_STREAMING)
        cancelButton.isEnabled = isStreaming
    }

    private fun updateStreamCounts() {
        streamCountLabel.text = "Sent: ${streamingService.sentCount} | Received: ${streamingService.receivedCount}"
    }

    private fun setupStreamObservation() {
        scope.launch {
            streamingService.state.collect { state ->
                SwingUtilities.invokeLater {
                    streamStateLabel.text = state.name
                    streamStateLabel.icon = StreamStateIcon(state)

                    if (state == GrpcStreamState.COMPLETED || state == GrpcStreamState.ERROR) {
                        updateStreamingButtons(false)
                    }
                }
            }
        }

        scope.launch {
            streamingService.messages.collect { message ->
                messageList.addMessage(message)
                SwingUtilities.invokeLater {
                    updateStreamCounts()
                }
            }
        }
    }

    // ─── Step 3.5: Display response ────────────────────────────────────────────

    private fun displayResult(result: GrpcCallResult, serviceName: String, methodName: String) {
        when (result) {
            is GrpcCallResult.Success -> {
                responseStatusLabel.text = "✅ ${result.statusCode}  ·  $serviceName/$methodName"
                try {
                    val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    val element = com.google.gson.JsonParser.parseString(result.responseJson)
                    responseBodyArea.text = gson.toJson(element)
                } catch (_: Exception) {
                    responseBodyArea.text = result.responseJson
                }
                responseMetaArea.text = result.trailingMetadata.entries
                    .joinToString("\n") { (k, v) -> "$k: $v" }
                    .ifEmpty { "(none)" }
                statusLabel.text = "OK — $serviceName/$methodName"
            }
            is GrpcCallResult.Failure -> {
                responseStatusLabel.text = "❌ ${result.statusCode}  ·  $serviceName/$methodName"
                responseBodyArea.text = result.userMessage
                responseMetaArea.text = "(none)"
                statusLabel.text = "Error: ${result.statusCode}"
            }
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun buildMetadataMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until metadataTableModel.rowCount) {
            val key = metadataTableModel.getValueAt(i, 0)?.toString()?.trim()
            val value = metadataTableModel.getValueAt(i, 1)?.toString()?.trim()
            if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                map[key] = value
            }
        }
        return map
    }

    private fun clearServiceComboBoxes() {
        (serviceComboBox.model as DefaultComboBoxModel<String>).removeAllElements()
        (methodComboBox.model as DefaultComboBoxModel<String>).removeAllElements()
        sendButton.isEnabled = false
    }

    /**
     * Loads raw FileDescriptors from a proto file for use in real gRPC calls.
     * This reuses protoc via a temp descriptor set file.
     */
    private fun loadFileDescriptorsFromProto(
        protoFile: File
    ): Map<String, com.google.protobuf.Descriptors.FileDescriptor> {
        val out = java.nio.file.Files.createTempFile("grpc_ui_desc_", ".pb").toFile()
        out.deleteOnExit()
        val cmd = listOf(
            "protoc",
            "--proto_path=${protoFile.parentFile.absolutePath}",
            "--descriptor_set_out=${out.absolutePath}",
            "--include_imports",
            protoFile.absolutePath
        )
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().readText() // drain output
        process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
        if (process.exitValue() != 0) return emptyMap()

        val fds = com.google.protobuf.DescriptorProtos.FileDescriptorSet.parseFrom(out.readBytes())
        val protoByName = fds.fileList.associateBy { it.name }
        val resolved = mutableMapOf<String, com.google.protobuf.Descriptors.FileDescriptor>()

        fun resolve(proto: com.google.protobuf.DescriptorProtos.FileDescriptorProto) {
            if (resolved.containsKey(proto.name)) return
            proto.dependencyList.mapNotNull { protoByName[it] }.forEach { resolve(it) }
            val deps = proto.dependencyList.mapNotNull { resolved[it] }.toTypedArray()
            resolved[proto.name] = com.google.protobuf.Descriptors.FileDescriptor.buildFrom(proto, deps)
        }

        fds.fileList.forEach { resolve(it) }
        return resolved
    }

    // Disposable implementation
    override fun dispose() {
        streamingService.dispose()
        grpcService.shutdown()
        scope.cancel()
    }

    // ─── Custom UI components ────────────────────────────────────────────────────

    private class StreamStateIcon(private val state: GrpcStreamState) : javax.swing.Icon {
        override fun paintIcon(c: java.awt.Component?, g: java.awt.Graphics?, x: Int, y: Int) {
            g?.color = when (state) {
                GrpcStreamState.STREAMING -> Color(0, 180, 0)
                GrpcStreamState.IDLE -> Color(180, 180, 180)
                GrpcStreamState.COMPLETED -> Color(100, 100, 180)
                GrpcStreamState.ERROR -> Color(200, 0, 0)
            }
            g?.fillOval(x + 2, y + 2, 8, 8)
        }

        override fun getIconWidth(): Int = 12
        override fun getIconHeight(): Int = 12
    }
}

package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.WebSocketMessage
import com.github.dhzhu.amateurpostman.models.WebSocketMessageType
import com.github.dhzhu.amateurpostman.models.WebSocketState
import com.github.dhzhu.amateurpostman.services.WebSocketServiceImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.util.Base64
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

/**
 * WebSocketPanel — WebSocket client UI for connecting and messaging.
 *
 * Features:
 * - High-performance message list with JSON syntax highlighting
 * - Disposable pattern for proper resource cleanup
 * - Message count limiting to prevent memory issues
 *
 * Layout:
 *   ┌────────────────────────────────────────────────────────────────┐
 *   │ URL: [ws://__________] Protocol: [ws/wss] Headers [Edit]       │
 *   │ [Connect] [Disconnect]                                          │
 *   ├────────────────────────────────────────────────────────────────┤
 *   │ Status: ● Connected | Messages: 42 | Sent: 10 | Received: 32   │
 *   ├────────────────────────────────────────────────────────────────┤
 *   │ Messages (scrollable list with JSON highlighting)              │
 *   │ ┌────────────────────────────────────────────────────────────┐ │
 *   │ │ → {"type":"subscribe","channel":"prices"}                  │ │
 *   │ │ ← {"type":"data","value":123.45}                           │ │
 *   │ │ ← {"type":"data","value":123.46}                           │ │
 *   │ └────────────────────────────────────────────────────────────┘ │
 *   ├────────────────────────────────────────────────────────────────┤
 *   │ [Text|Binary] [________________] [Send] [Clear]                │
 *   └────────────────────────────────────────────────────────────────┘
 */
class WebSocketPanel(private val project: Project) : Disposable {

    private val service = WebSocketServiceImpl()
    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

    // UI components
    private lateinit var urlField: JBTextField
    private lateinit var protocolComboBox: ComboBox<String>
    private lateinit var connectButton: JButton
    private lateinit var disconnectButton: JButton
    private lateinit var statusLabel: JLabel
    private lateinit var messageCountLabel: JLabel
    private lateinit var sentCountLabel: JLabel
    private lateinit var receivedCountLabel: JLabel

    // Headers table
    private lateinit var headersTableModel: DefaultTableModel
    private lateinit var headersTable: JBTable

    // High-performance message list with JSON syntax highlighting
    private lateinit var messageList: StreamMessageList<WebSocketMessage>

    // Input area
    private lateinit var messageInputArea: JBTextArea
    private lateinit var sendTextButton: JButton
    private lateinit var clearButton: JButton
    private lateinit var binaryModeCheckBox: JCheckBox

    fun createPanel(): JPanel {
        val root = JPanel(BorderLayout(0, 4))
        root.border = JBUI.Borders.empty(6)

        root.add(createTopBar(), BorderLayout.NORTH)
        root.add(createMainContent(), BorderLayout.CENTER)
        root.add(createStatusBar(), BorderLayout.SOUTH)

        setupStateObservation()

        return root
    }

    private fun createTopBar(): JPanel {
        val panel = JPanel(BorderLayout(0, 4))

        // Row 1: URL input
        val urlRow = JPanel(BorderLayout(4, 0))
        urlRow.add(JBLabel("URL:"), BorderLayout.WEST)

        val urlInputPanel = JPanel(BorderLayout(4, 0))
        protocolComboBox = ComboBox(DefaultComboBoxModel(arrayOf("wss://", "ws://")))
        protocolComboBox.selectedItem = "wss://"
        protocolComboBox.preferredSize = Dimension(70, protocolComboBox.preferredSize.height)
        urlInputPanel.add(protocolComboBox, BorderLayout.WEST)

        urlField = JBTextField("echo.websocket.org")
        urlField.toolTipText = "WebSocket server URL"
        urlInputPanel.add(urlField, BorderLayout.CENTER)

        urlRow.add(urlInputPanel, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        connectButton = JButton("Connect")
        connectButton.addActionListener { connect() }
        disconnectButton = JButton("Disconnect")
        disconnectButton.isEnabled = false
        disconnectButton.addActionListener { disconnect() }
        buttonPanel.add(connectButton)
        buttonPanel.add(disconnectButton)
        urlRow.add(buttonPanel, BorderLayout.EAST)

        // Row 2: Headers
        val headersRow = JPanel(BorderLayout(4, 0))
        headersRow.add(JBLabel("Headers:"), BorderLayout.WEST)

        headersTableModel = DefaultTableModel(arrayOf("Key", "Value"), 0)
        headersTable = JBTable(headersTableModel)
        headersTable.setShowGrid(true)
        headersTable.preferredSize = Dimension(0, 60)

        val headersButtonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        val addHeaderButton = JButton("Add")
        addHeaderButton.addActionListener { headersTableModel.addRow(arrayOf("", "")) }
        val removeHeaderButton = JButton("Remove")
        removeHeaderButton.addActionListener {
            val sel = headersTable.selectedRow
            if (sel >= 0) headersTableModel.removeRow(sel)
        }
        headersButtonPanel.add(addHeaderButton)
        headersButtonPanel.add(removeHeaderButton)

        val headersContainer = JPanel(BorderLayout())
        headersContainer.add(JBScrollPane(headersTable).apply { preferredSize = Dimension(0, 60) }, BorderLayout.CENTER)
        headersContainer.add(headersButtonPanel, BorderLayout.EAST)
        headersRow.add(headersContainer, BorderLayout.CENTER)

        panel.add(urlRow, BorderLayout.NORTH)
        panel.add(headersRow, BorderLayout.SOUTH)

        return panel
    }

    private fun createMainContent(): JPanel {
        val panel = JPanel(BorderLayout(0, 4))

        // High-performance message list with syntax highlighting
        val messagesPanel = JPanel(BorderLayout())
        messagesPanel.border = BorderFactory.createTitledBorder("Messages")

        messageList = StreamMessageList(maxMessages = 1000)
        messagesPanel.add(messageList, BorderLayout.CENTER)

        // Input area
        val inputPanel = JPanel(BorderLayout(4, 0))
        inputPanel.border = JBUI.Borders.emptyTop(4)

        val inputToolbar = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        binaryModeCheckBox = JCheckBox("Binary (Base64)")
        inputToolbar.add(binaryModeCheckBox)

        messageInputArea = JBTextArea()
        messageInputArea.font = Font("Monospaced", Font.PLAIN, 13)
        messageInputArea.rows = 3
        messageInputArea.toolTipText = "Enter message to send"

        sendTextButton = JButton("Send")
        sendTextButton.isEnabled = false
        sendTextButton.addActionListener { sendMessage() }

        clearButton = JButton("Clear")
        clearButton.addActionListener { clearMessages() }

        inputToolbar.add(sendTextButton)
        inputToolbar.add(clearButton)

        inputPanel.add(inputToolbar, BorderLayout.WEST)
        inputPanel.add(JBScrollPane(messageInputArea), BorderLayout.CENTER)

        panel.add(messagesPanel, BorderLayout.CENTER)
        panel.add(inputPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun createStatusBar(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 12, 2))
        panel.border = JBUI.Borders.emptyTop(4)

        statusLabel = JLabel("Disconnected")
        statusLabel.icon = StatusIcon(WebSocketState.DISCONNECTED)
        panel.add(statusLabel)

        panel.add(JLabel("|"))

        messageCountLabel = JLabel("Messages: 0")
        panel.add(messageCountLabel)

        sentCountLabel = JLabel("Sent: 0")
        panel.add(sentCountLabel)

        receivedCountLabel = JLabel("Received: 0")
        panel.add(receivedCountLabel)

        return panel
    }

    private fun setupStateObservation() {
        scope.launch {
            service.state.collect { state ->
                SwingUtilities.invokeLater {
                    updateUiForState(state)
                }
            }
        }

        scope.launch {
            service.messages.collect { message ->
                messageList.addMessage(message)
                SwingUtilities.invokeLater {
                    updateCounts()
                }
            }
        }
    }

    private fun updateUiForState(state: WebSocketState) {
        statusLabel.text = state.name
        statusLabel.icon = StatusIcon(state)

        connectButton.isEnabled = state == WebSocketState.DISCONNECTED || state == WebSocketState.CLOSED
        disconnectButton.isEnabled = state == WebSocketState.CONNECTED
        sendTextButton.isEnabled = state == WebSocketState.CONNECTED
        urlField.isEnabled = state == WebSocketState.DISCONNECTED || state == WebSocketState.CLOSED
        protocolComboBox.isEnabled = state == WebSocketState.DISCONNECTED || state == WebSocketState.CLOSED
    }

    private fun updateCounts() {
        messageCountLabel.text = "Messages: ${service.messageHistory.size}"
        sentCountLabel.text = "Sent: ${service.sentCount}"
        receivedCountLabel.text = "Received: ${service.receivedCount}"
    }

    private fun connect() {
        val protocol = protocolComboBox.selectedItem as String
        val host = urlField.text.trim()

        if (host.isEmpty()) {
            statusLabel.text = "Please enter a URL"
            return
        }

        val url = "$protocol$host"

        // Collect headers
        val headers = mutableMapOf<String, String>()
        for (i in 0 until headersTableModel.rowCount) {
            val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
            val value = headersTableModel.getValueAt(i, 1)?.toString()?.trim()
            if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                headers[key] = value
            }
        }

        messageList.clearMessages()
        service.clearHistory()
        updateCounts()

        service.connect(url, headers)
    }

    private fun disconnect() {
        service.disconnect()
    }

    private fun sendMessage() {
        val text = messageInputArea.text.trim()
        if (text.isEmpty()) return

        if (binaryModeCheckBox.isSelected) {
            try {
                val data = Base64.getDecoder().decode(text)
                service.sendBinary(data)
            } catch (e: IllegalArgumentException) {
                statusLabel.text = "Invalid Base64: ${e.message}"
                return
            }
        } else {
            service.send(text)
        }

        messageInputArea.text = ""
    }

    private fun clearMessages() {
        messageList.clearMessages()
        service.clearHistory()
        updateCounts()
    }

    // Disposable implementation
    override fun dispose() {
        service.dispose()
        scope.cancel()
    }

    // Status icon indicator
    private class StatusIcon(private val state: WebSocketState) : javax.swing.Icon {
        override fun paintIcon(c: java.awt.Component?, g: java.awt.Graphics?, x: Int, y: Int) {
            g?.color = when (state) {
                WebSocketState.CONNECTED -> Color(0, 180, 0)
                WebSocketState.CONNECTING -> Color(255, 165, 0)
                WebSocketState.CLOSING -> Color(255, 165, 0)
                WebSocketState.DISCONNECTED -> Color(180, 180, 180)
                WebSocketState.CLOSED -> Color(180, 180, 180)
            }
            g?.fillOval(x + 2, y + 2, 8, 8)
        }

        override fun getIconWidth(): Int = 12
        override fun getIconHeight(): Int = 12
    }
}
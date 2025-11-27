package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.services.HttpRequestService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import java.awt.Font
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

/** Main panel for the Amateur-Postman tool window */
class PostmanToolWindowPanel(private val project: Project) {

    private val httpService = project.service<HttpRequestService>()
    private val scope = CoroutineScope(Dispatchers.Swing)

    // UI State
    private var selectedMethod = HttpMethod.GET
    private var urlText = ""
    private var requestBodyText = ""
    private var responseText = ""
    private var statusText = "Ready"

    // UI Components
    private lateinit var urlField: com.intellij.ui.components.JBTextField
    private lateinit var methodComboBox: com.intellij.openapi.ui.ComboBox<HttpMethod>
    private lateinit var headersTable: JBTable
    private lateinit var requestBodyArea: JBTextArea
    private lateinit var responseArea: JBTextArea
    private lateinit var statusLabel: javax.swing.JLabel

    private val headersTableModel = DefaultTableModel(arrayOf("Key", "Value"), 0)

    fun createPanel(): JPanel {
        return panel {
            // Request Section
            group("Request") {
                row {
                    label("Method:")
                    methodComboBox =
                            comboBox(HttpMethod.values().toList())
                                    .bindItem(
                                            { selectedMethod },
                                            { selectedMethod = it ?: HttpMethod.GET }
                                    )
                                    .component

                    label("URL:")
                    urlField =
                            textField()
                                    .bindText({ urlText }, { urlText = it })
                                    .columns(40)
                                    .resizableColumn()
                                    .component

                    button("Send") { sendRequest() }
                }

                // Headers Section
                row {
                    panel {
                                row { label("Headers:").bold() }
                                row {
                                    headersTable =
                                            JBTable(headersTableModel).apply { setShowGrid(true) }
                                    cell(JBScrollPane(headersTable))
                                            .resizableColumn()
                                            .align(AlignX.FILL)
                                }
                                row {
                                    button("Add Header") {
                                        headersTableModel.addRow(arrayOf("", ""))
                                    }
                                    button("Remove Selected") {
                                        val selectedRow = headersTable.selectedRow
                                        if (selectedRow >= 0) {
                                            headersTableModel.removeRow(selectedRow)
                                        }
                                    }
                                }
                            }
                            .resizableColumn()
                            .align(AlignX.FILL)
                }

                // Request Body Section
                row {
                    panel {
                                row { label("Request Body:").bold() }
                                row {
                                    requestBodyArea =
                                            JBTextArea(8, 60).apply {
                                                lineWrap = true
                                                wrapStyleWord = true
                                                font = Font("Monospaced", Font.PLAIN, 12)
                                            }
                                    cell(JBScrollPane(requestBodyArea))
                                            .resizableColumn()
                                            .align(AlignX.FILL)
                                }
                            }
                            .resizableColumn()
                            .align(AlignX.FILL)
                }
            }

            separator()

            // Response Section
            group("Response") {
                row { statusLabel = label(statusText).bold().component }
                row {
                    panel {
                                row {
                                    responseArea =
                                            JBTextArea(15, 60).apply {
                                                isEditable = false
                                                lineWrap = true
                                                wrapStyleWord = true
                                                font = Font("Monospaced", Font.PLAIN, 12)
                                            }
                                    cell(JBScrollPane(responseArea))
                                            .resizableColumn()
                                            .align(AlignX.FILL)
                                }
                            }
                            .resizableColumn()
                            .align(AlignX.FILL)
                }
            }
        }
                .apply {
                    // Initialize with one empty header row
                    headersTableModel.addRow(arrayOf("Content-Type", "application/json"))
                }
    }

    private fun sendRequest() {
        statusLabel.text = "Sending request..."
        responseArea.text = ""

        scope.launch {
            try {
                // Collect headers from table
                val headers = mutableMapOf<String, String>()
                for (i in 0 until headersTableModel.rowCount) {
                    val key = headersTableModel.getValueAt(i, 0)?.toString()?.trim()
                    val value = headersTableModel.getValueAt(i, 1)?.toString()?.trim()
                    if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                        headers[key] = value
                    }
                }

                // Determine content type
                val contentType = headers["Content-Type"] ?: "application/json"

                // Build request
                val request =
                        HttpRequest(
                                url = urlText.trim(),
                                method = selectedMethod,
                                headers = headers,
                                body =
                                        if (requestBodyArea.text.isNotBlank()) requestBodyArea.text
                                        else null,
                                contentType = contentType
                        )

                // Execute request
                val response = httpService.executeRequest(request)

                // Display response
                displayResponse(response)
            } catch (e: Exception) {
                statusLabel.text = "Error: ${e.message}"
                responseArea.text =
                        "Error executing request:\n${e.message}\n\n${e.stackTraceToString()}"
            }
        }
    }

    private fun displayResponse(response: HttpResponse) {
        val statusColor = if (response.isSuccessful) "green" else "red"
        statusLabel.text =
                "Status: ${response.statusCode} ${response.statusMessage} | Time: ${response.duration}ms"

        // Format response
        val formattedResponse = buildString {
            appendLine("HTTP ${response.statusCode} ${response.statusMessage}")
            appendLine("Duration: ${response.duration}ms")
            appendLine()
            appendLine("=== Headers ===")
            response.headers.forEach { (key, values) ->
                values.forEach { value -> appendLine("$key: $value") }
            }
            appendLine()
            appendLine("=== Body ===")
            appendLine(formatResponseBody(response.body, response.headers))
        }

        responseArea.text = formattedResponse
    }

    private fun formatResponseBody(body: String, headers: Map<String, List<String>>): String {
        // Check if response is JSON
        val contentType = headers["Content-Type"]?.firstOrNull() ?: ""

        return if (contentType.contains("application/json", ignoreCase = true)) {
            try {
                // Try to pretty-print JSON
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
}

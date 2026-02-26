package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.CollectionItem
import com.github.dhzhu.amateurpostman.models.RequestCollection
import com.github.dhzhu.amateurpostman.services.EnvironmentService
import com.github.dhzhu.amateurpostman.services.ScriptExecutionService
import com.github.dhzhu.amateurpostman.utils.VariableResolver
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.ListCellRenderer

/**
 * Result of running a single request in a collection.
 *
 * @property itemName The name of the request item
 * @property passed Whether the request was successful and tests passed
 * @property statusCode HTTP status code (or -1 if request failed)
 * @property duration Request duration in milliseconds
 * @property testResults Test results if tests were defined
 * @property error Error message if the request failed
 */
data class CollectionRunResult(
    val itemName: String,
    val passed: Boolean,
    val statusCode: Int = -1,
    val duration: Long = 0,
    val testResults: com.github.dhzhu.amateurpostman.services.TestResult? = null,
    val error: String? = null
)

/**
 * Statistics for a collection run.
 *
 * @property total Total number of requests
 * @property passed Number of passed requests (successful + tests passed)
 * @property failed Number of failed requests
 * @property totalDuration Total duration in milliseconds
 */
data class CollectionRunStats(
    val total: Int = 0,
    val passed: Int = 0,
    val failed: Int = 0,
    val totalDuration: Long = 0
) {
    /**
     * Returns the pass rate as a percentage.
     */
    fun getPassRate(): Double {
        return if (total > 0) (passed.toDouble() / total.toDouble()) * 100.0 else 0.0
    }

    fun getSummary(): String {
        return "Total: $total | Passed: $passed | Failed: $failed | Time: ${totalDuration}ms | Pass Rate: ${"%.1f".format(getPassRate())}%"
    }
}

/**
 * Dialog for running a collection of requests with test reporting.
 */
class CollectionRunnerDialog(
    private val project: Project,
    private val collection: RequestCollection
) : DialogWrapper(project) {

    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())
    private var runJob: Job? = null

    private val environmentService = project.service<EnvironmentService>()
    private val scriptExecutionService = project.service<ScriptExecutionService>()

    // UI Components
    private lateinit var resultsList: JBList<CollectionRunResult>
    private val resultsListModel = DefaultListModel<CollectionRunResult>()
    private lateinit var statsLabel: JLabel
    private lateinit var progressBar: JProgressBar
    private lateinit var detailsArea: JTextArea
    private lateinit var runButton: JButton
    private lateinit var closeButton: JButton

    // Run state
    private val results = mutableListOf<CollectionRunResult>()
    private var isRunning = false

    init {
        title = "Collection Runner: ${collection.name}"
        setOKButtonText("Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)
        panel.preferredSize = java.awt.Dimension(800, 600)

        // Top panel with statistics and controls
        val topPanel = JPanel(BorderLayout())
        topPanel.border = JBUI.Borders.empty(5)

        // Statistics label
        statsLabel = JLabel("Ready to run ${getRequestCount(collection)} requests")
        statsLabel.font = Font("Dialog", Font.BOLD, 14)
        topPanel.add(statsLabel, BorderLayout.NORTH)

        // Progress bar
        progressBar = JProgressBar(0, getRequestCount(collection))
        progressBar.isStringPainted = true
        progressBar.string = "0 / ${getRequestCount(collection)}"
        topPanel.add(progressBar, BorderLayout.CENTER)

        // Control buttons
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        runButton = JButton("Run Collection")
        runButton.addActionListener { startRun() }
        closeButton = JButton("Close")
        closeButton.addActionListener {
            runJob?.cancel()
            super.close(DialogWrapper.OK_EXIT_CODE)
        }
        buttonPanel.add(runButton)
        buttonPanel.add(closeButton)
        topPanel.add(buttonPanel, BorderLayout.SOUTH)

        panel.add(topPanel, BorderLayout.NORTH)

        // Center split pane with results list and details
        val splitPane = com.intellij.ui.JBSplitter(false, 0.5f)

        // Results list
        resultsList = JBList(resultsListModel)
        resultsList.cellRenderer = ResultCellRenderer()
        resultsList.addListSelectionListener {
            updateDetails()
        }
        splitPane.firstComponent = JBScrollPane(resultsList)

        // Details area
        detailsArea = JTextArea()
        detailsArea.isEditable = false
        detailsArea.font = Font("Monospaced", Font.PLAIN, 12)
        detailsArea.text = "Select an item to view details"
        splitPane.secondComponent = JBScrollPane(detailsArea)

        panel.add(splitPane, BorderLayout.CENTER)

        return panel
    }

    override fun dispose() {
        runJob?.cancel()
        super.dispose()
    }

    /**
     * Counts the total number of requests in a collection (recursive).
     */
    private fun getRequestCount(collection: RequestCollection): Int {
        return countRequestsRecursive(collection.items)
    }

    private fun countRequestsRecursive(items: List<CollectionItem>): Int {
        var count = 0
        items.forEach { item ->
            when (item) {
                is CollectionItem.Request -> count++
                is CollectionItem.Folder -> count += countRequestsRecursive(item.children)
            }
        }
        return count
    }

    /**
     * Gets all requests from a collection in order (recursive).
     */
    private fun getAllRequests(items: List<CollectionItem>): List<CollectionItem.Request> {
        val result = mutableListOf<CollectionItem.Request>()
        items.forEach { item ->
            when (item) {
                is CollectionItem.Request -> result.add(item)
                is CollectionItem.Folder -> result.addAll(getAllRequests(item.children))
            }
        }
        return result
    }

    /**
     * Starts the collection run.
     */
    private fun startRun() {
        if (isRunning) {
            // Cancel running
            runJob?.cancel()
            isRunning = false
            runButton.text = "Run Collection"
            closeButton.isEnabled = true
            return
        }

        // Reset state
        results.clear()
        resultsListModel.clear()
        progressBar.value = 0
        detailsArea.text = "Running..."
        isRunning = true
        runButton.text = "Stop"
        closeButton.isEnabled = false

        runJob = scope.launch {
            try {
                val allRequests = getAllRequests(collection.items)
                val totalRequests = allRequests.size
                progressBar.maximum = totalRequests

                var passed = 0
                var failed = 0
                var totalDuration = 0L

                allRequests.forEachIndexed { index, item ->
                    if (!isRunning) return@launch

                    val result = runRequest(item)
                    results.add(result)
                    resultsListModel.addElement(result)

                    progressBar.value = index + 1
                    progressBar.string = "${index + 1} / $totalRequests"

                    if (result.passed) passed++ else failed++
                    totalDuration += result.duration

                    updateStats(totalRequests, passed, failed, totalDuration)
                }

                detailsArea.text = buildFinalSummary()
                isRunning = false
                runButton.text = "Run Collection"
                closeButton.isEnabled = true

            } catch (e: Exception) {
                detailsArea.text = "Error: ${e.message}\n\n${e.stackTraceToString()}"
                isRunning = false
                runButton.text = "Run Collection"
                closeButton.isEnabled = true
            }
        }
    }

    /**
     * Runs a single request.
     */
    private suspend fun runRequest(requestItem: com.github.dhzhu.amateurpostman.models.CollectionItem.Request): CollectionRunResult {
        val startTime = System.currentTimeMillis()

        return try {
            // Execute pre-request script if defined
            if (requestItem.preRequestScript.isNotBlank()) {
                scriptExecutionService.executePreRequestScript(requestItem.preRequestScript)
            }

            // Resolve variables
            val variables = environmentService.getAllVariables()
            val resolvedRequest = VariableResolver.substitute(requestItem.request, variables)

            // Execute request
            val response = project.service<com.github.dhzhu.amateurpostman.services.HttpRequestService>()
                .executeRequest(resolvedRequest)

            val duration = System.currentTimeMillis() - startTime

            // Run tests if defined
            val testResult = if (requestItem.testScript.isNotBlank()) {
                scriptExecutionService.executeTestScript(requestItem.testScript, response)
            } else {
                null
            }

            val passed = response.statusCode in 200..299 && (testResult == null || testResult.passed)

            CollectionRunResult(
                itemName = requestItem.name,
                passed = passed,
                statusCode = response.statusCode,
                duration = duration,
                testResults = testResult
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            CollectionRunResult(
                itemName = requestItem.name,
                passed = false,
                statusCode = -1,
                duration = duration,
                error = e.message
            )
        }
    }

    /**
     * Updates the statistics label.
     */
    private fun updateStats(total: Int, passed: Int, failed: Int, totalDuration: Long) {
        val stats = CollectionRunStats(total, passed, failed, totalDuration)
        statsLabel.text = stats.getSummary()
    }

    /**
     * Updates the details area with the selected result.
     */
    private fun updateDetails() {
        val selected = resultsList.selectedValue
        if (selected != null) {
            detailsArea.text = buildResultDetails(selected)
        }
    }

    /**
     * Builds detailed text for a result.
     */
    private fun buildResultDetails(result: CollectionRunResult): String {
        return buildString {
            appendLine("Request: ${result.itemName}")
            appendLine("Status: ${if (result.passed) "PASSED" else "FAILED"}")
            appendLine("Status Code: ${result.statusCode}")
            appendLine("Duration: ${result.duration}ms")
            if (result.error != null) {
                appendLine("Error: ${result.error}")
            }
            if (result.testResults != null) {
                appendLine()
                appendLine("Test Results:")
                result.testResults.results.forEach { assertion ->
                    val status = if (assertion.passed) "✓ PASS" else "✗ FAIL"
                    appendLine("  $status: ${assertion.name}")
                    if (assertion.message.isNotEmpty()) {
                        appendLine("      ${assertion.message}")
                    }
                }
            }
        }
    }

    /**
     * Builds the final summary text.
     */
    private fun buildFinalSummary(): String {
        return buildString {
            appendLine("=".repeat(60))
            appendLine("Collection Run Complete")
            appendLine("=".repeat(60))
            appendLine(statsLabel.text)
            appendLine()
            results.forEach { result ->
                val status = if (result.passed) "✓" else "✗"
                appendLine("$status ${result.itemName} (${result.statusCode}) - ${result.duration}ms")
            }
        }
    }

    /**
     * Custom cell renderer for results list.
     */
    private class ResultCellRenderer : ListCellRenderer<CollectionRunResult> {
        override fun getListCellRendererComponent(
            list: JList<out CollectionRunResult>,
            value: CollectionRunResult,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = JBUI.Borders.empty(5)

            val nameLabel = JLabel(value.itemName)
            nameLabel.font = Font("Dialog", Font.BOLD, 12)

            val statusLabel = JLabel(
                when {
                    value.error != null -> "Error: ${value.error}"
                    value.passed -> "✓ Passed (${value.statusCode}) - ${value.duration}ms"
                    else -> "✗ Failed (${value.statusCode}) - ${value.duration}ms"
                }
            )
            statusLabel.font = Font("Dialog", Font.PLAIN, 11)

            if (isSelected) {
                panel.background = list.selectionBackground
                nameLabel.foreground = list.selectionForeground
                statusLabel.foreground = list.selectionForeground
            } else {
                panel.background = list.background
                nameLabel.foreground = list.foreground
                statusLabel.foreground = if (value.passed) Color(0, 150, 0) else Color(200, 0, 0)
            }

            panel.isOpaque = true
            panel.add(nameLabel)
            panel.add(statusLabel)

            return panel
        }
    }
}

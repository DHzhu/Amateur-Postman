package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Panel for displaying HTTP request profiling data.
 * Shows both a waterfall timeline visualization and detailed timing information.
 */
class ProfilingPanel : JPanel() {

    private val profilingTextArea: JBTextArea
    private val noDataLabel: JLabel
    private val timelinePanel: ProfilingTimelinePanel
    private val contentPanel: JPanel

    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty(10)

        // Label shown when no profiling data is available
        noDataLabel = JLabel("No profiling data available")
        noDataLabel.horizontalAlignment = SwingConstants.CENTER
        noDataLabel.font = Font("Dialog", Font.ITALIC, 12)

        // Text area for displaying profiling data
        profilingTextArea = createProfilingTextArea()
        val textScrollPane = JBScrollPane(profilingTextArea)

        // Timeline panel for waterfall visualization
        timelinePanel = ProfilingTimelinePanel()

        // Content panel with split view (timeline on top, text details below)
        val splitter = JBSplitter(false, 0.6f)
        splitter.dividerWidth = 2
        splitter.divider.background = Color(60, 60, 60)
        splitter.firstComponent = timelinePanel
        splitter.secondComponent = textScrollPane

        contentPanel = JPanel(BorderLayout())
        contentPanel.add(noDataLabel, BorderLayout.NORTH)
        contentPanel.add(splitter, BorderLayout.CENTER)

        add(contentPanel, BorderLayout.CENTER)
    }

    /**
     * Update the panel with new profiling data.
     */
    fun updateProfilingData(data: HttpProfilingData) {
        val displayModel = ProfilingDisplayModel.fromProfilingData(data)

        if (displayModel.hasProfilingData) {
            noDataLabel.isVisible = false
            contentPanel.isVisible = true

            // Update text area
            profilingTextArea.text = displayModel.toSummary()

            // Update timeline
            timelinePanel.updateProfilingData(data)
        } else {
            noDataLabel.isVisible = true
            noDataLabel.text = "No profiling data available"
            contentPanel.isVisible = false
        }

        revalidate()
        repaint()
    }

    /**
     * Clear the profiling data display.
     */
    fun clear() {
        noDataLabel.isVisible = true
        noDataLabel.text = "No profiling data available"
        contentPanel.isVisible = false

        profilingTextArea.text = ""
        timelinePanel.clear()

        revalidate()
        repaint()
    }

    private fun createProfilingTextArea(): JBTextArea {
        val textArea = JBTextArea()
        textArea.isEditable = false
        textArea.font = Font("Monospaced", Font.PLAIN, 12)
        textArea.background = Color(43, 43, 43)
        textArea.foreground = Color(212, 212, 212)
        textArea.border = JBUI.Borders.empty(10)
        return textArea
    }
}

package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Custom component that renders a waterfall/Gantt chart visualization of HTTP profiling data.
 * Shows timing bars for DNS, TCP, SSL, TTFB, and Transfer phases.
 */
private class ProfilingWaterfallComponent(private var profilingData: HttpProfilingData) : JComponent() {

    // Theme-aware colors for different phases
    // Each JBColor takes (darkThemeColor, lightThemeColor)
    private val colorDNS = JBColor(Color(156, 220, 254), Color(100, 149, 237))    // Light blue / Cornflower blue
    private val colorTCP = JBColor(Color(181, 206, 168), Color(60, 179, 113))     // Light green / Medium sea green
    private val colorSSL = JBColor(Color(206, 145, 120), Color(205, 133, 63))     // Light orange / Peru
    private val colorTTFB = JBColor(Color(249, 168, 212), Color(219, 112, 147))   // Light pink / Pale violet red
    private val colorTransfer = JBColor(Color(86, 156, 214), Color(65, 105, 225))  // Blue / Royal blue
    private val colorBackground = JBColor(Color(43, 43, 43), Color(245, 245, 245))  // Dark gray / Light gray
    private val colorText = JBColor(Color(212, 212, 212), Color(50, 50, 50))        // Light gray / Dark gray
    private val colorGrid = JBColor(Color(60, 60, 60), Color(220, 220, 220))        // Dark gray / Light gray

    // Layout constants
    private val paddingLeft = 120
    private val paddingRight = 30
    private val paddingTop = 40
    private val paddingBottom = 30
    private val barHeight = 24
    private val barSpacing = 8
    private val minBarWidth = 2

    init {
        preferredSize = Dimension(600, calculateHeight())
    }

    fun updateData(data: HttpProfilingData) {
        profilingData = data
        preferredSize = Dimension(600, calculateHeight())
        revalidate()
        repaint()
    }

    private fun calculateHeight(): Int {
        if (profilingData == HttpProfilingData.Empty) {
            return 100
        }
        val phaseCount = countVisiblePhases()
        return paddingTop + paddingBottom + (phaseCount * (barHeight + barSpacing)) + 60
    }

    private fun countVisiblePhases(): Int {
        var count = 0
        if (profilingData.dnsDuration != null) count++
        if (profilingData.tcpDuration != null) count++
        if (profilingData.sslDuration != null) count++
        if (profilingData.ttfbDuration != null) count++
        // Always show total
        count++
        return count
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draw background
        g2d.color = colorBackground
        g2d.fillRect(0, 0, width, height)

        if (profilingData == HttpProfilingData.Empty) {
            drawNoDataMessage(g2d)
            return
        }

        // Draw title
        g2d.color = colorText
        g2d.font = Font("Dialog", Font.BOLD, 14)
        g2d.drawString("Request Timeline", paddingLeft, paddingTop - 15)

        // Calculate scaling
        val totalTime = profilingData.totalDuration.toDouble()
        val chartWidth = width - paddingLeft - paddingRight
        val scale = if (totalTime > 0) chartWidth / totalTime else 1.0

        // Draw time grid
        drawTimeGrid(g2d, chartWidth, totalTime)

        // Draw phases
        var y = paddingTop
        var cumulativeTime = 0.0

        // DNS phase
        if (profilingData.dnsDuration != null) {
            val duration = profilingData.dnsDuration!!.toDouble()
            drawPhaseBar(g2d, "DNS", cumulativeTime, duration, scale, y, colorDNS)
            y += barHeight + barSpacing
            cumulativeTime += duration
        }

        // TCP phase
        if (profilingData.tcpDuration != null) {
            val duration = profilingData.tcpDuration!!.toDouble()
            drawPhaseBar(g2d, "TCP", cumulativeTime, duration, scale, y, colorTCP)
            y += barHeight + barSpacing
            cumulativeTime += duration
        }

        // SSL phase
        if (profilingData.sslDuration != null) {
            val duration = profilingData.sslDuration!!.toDouble()
            drawPhaseBar(g2d, "SSL", cumulativeTime, duration, scale, y, colorSSL)
            y += barHeight + barSpacing
            cumulativeTime += duration
        }

        // TTFB phase (includes request sending + server processing + first byte)
        if (profilingData.ttfbDuration != null) {
            // Calculate waiting time (TTFB minus previous phases)
            val previousPhasesTime = (profilingData.dnsDuration?.toDouble() ?: 0.0) +
                                    (profilingData.tcpDuration?.toDouble() ?: 0.0) +
                                    (profilingData.sslDuration?.toDouble() ?: 0.0)
            val ttfb = profilingData.ttfbDuration!!
            val waitingTime = (ttfb.toDouble() - previousPhasesTime).coerceAtLeast(0.0)
            drawPhaseBar(g2d, "Waiting (TTFB)", cumulativeTime, waitingTime, scale, y, colorTTFB)
            y += barHeight + barSpacing
            cumulativeTime += waitingTime
        }

        // Total time bar
        g2d.color = colorText
        g2d.font = Font("Monospaced", Font.PLAIN, 11)
        val totalLabel = String.format("Total: %.1f ms", totalTime)
        g2d.drawString(totalLabel, paddingLeft, y + barHeight / 2 + 4)

        // Connection reuse indicator
        if (profilingData.connectionReused) {
            g2d.color = Color(181, 206, 168)
            g2d.font = Font("Dialog", Font.ITALIC, 11)
            g2d.drawString("✓ Connection Reused", paddingLeft, y + barHeight + 20)
        }
    }

    private fun drawPhaseBar(
        g2d: Graphics2D,
        label: String,
        startTime: Double,
        duration: Double,
        scale: Double,
        y: Int,
        color: Color
    ) {
        val barWidth = (duration * scale).toInt().coerceAtLeast(minBarWidth)
        val x = paddingLeft + (startTime * scale).toInt()

        // Draw label
        g2d.color = colorText
        g2d.font = Font("Dialog", Font.PLAIN, 12)
        g2d.drawString(label, 10, y + barHeight / 2 + 4)

        // Draw bar background
        g2d.color = Color(60, 60, 60)
        g2d.fillRoundRect(x, y, barWidth, barHeight, 4, 4)

        // Draw bar
        g2d.color = color
        g2d.fillRoundRect(x, y, barWidth, barHeight, 4, 4)

        // Draw duration text on bar if wide enough
        if (barWidth > 50) {
            g2d.color = Color(0, 0, 0)
            g2d.font = Font("Dialog", Font.BOLD, 10)
            val text = String.format("%.1f ms", duration)
            val textWidth = g2d.fontMetrics.stringWidth(text)
            if (textWidth < barWidth - 4) {
                g2d.drawString(text, x + (barWidth - textWidth) / 2, y + barHeight / 2 + 4)
            }
        }
    }

    private fun drawTimeGrid(g2d: Graphics2D, chartWidth: Int, totalTime: Double) {
        g2d.color = colorGrid
        g2d.font = Font("Dialog", Font.PLAIN, 9)

        // Determine appropriate time interval
        val interval = when {
            totalTime < 100 -> 20.0
            totalTime < 500 -> 100.0
            totalTime < 1000 -> 200.0
            else -> 500.0
        }

        val gridHeight = countVisiblePhases() * (barHeight + barSpacing)
        var time = 0.0
        while (time <= totalTime) {
            val x = paddingLeft + (time / totalTime * chartWidth).toInt()
            g2d.drawLine(x, paddingTop - 5, x, paddingTop + gridHeight)

            // Draw time label
            val label = String.format("%.0f ms", time)
            val labelWidth = g2d.fontMetrics.stringWidth(label)
            g2d.drawString(label, x - labelWidth / 2, paddingTop - 8)

            time += interval
        }
    }

    private fun drawNoDataMessage(g2d: Graphics2D) {
        g2d.color = colorText
        g2d.font = Font("Dialog", Font.ITALIC, 12)
        val message = "No profiling data available"
        val messageWidth = g2d.fontMetrics.stringWidth(message)
        g2d.drawString(
            message,
            (width - messageWidth) / 2,
            height / 2
        )
    }
}

/**
 * Panel that wraps the ProfilingWaterfallComponent.
 */
class ProfilingTimelinePanel : JPanel() {

    private val waterfallComponent: ProfilingWaterfallComponent

    init {
        layout = java.awt.BorderLayout()
        border = JBUI.Borders.empty(10)
        waterfallComponent = ProfilingWaterfallComponent(HttpProfilingData.Empty)
        add(waterfallComponent, java.awt.BorderLayout.CENTER)
    }

    fun updateProfilingData(data: HttpProfilingData) {
        waterfallComponent.updateData(data)
    }

    fun clear() {
        waterfallComponent.updateData(HttpProfilingData.Empty)
    }
}

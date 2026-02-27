package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ProfilingTimelinePanel.
 * Tests the waterfall visualization component.
 */
class ProfilingTimelinePanelTest {

    /**
     * Test: Update panel with fresh connection data
     */
    @Test
    fun testUpdateWithFreshConnectionData() {
        val panel = ProfilingTimelinePanel()
        val data = HttpProfilingData(
            dnsDuration = 50,
            tcpDuration = 80,
            sslDuration = 120,
            ttfbDuration = 300,
            totalDuration = 450,
            connectionReused = false
        )

        // Should not throw any exception
        panel.updateProfilingData(data)
        assertNotNull("Panel should be created", panel)
    }

    /**
     * Test: Update panel with connection reuse data
     */
    @Test
    fun testUpdateWithConnectionReuseData() {
        val panel = ProfilingTimelinePanel()
        val data = HttpProfilingData(
            dnsDuration = null,
            tcpDuration = null,
            sslDuration = null,
            ttfbDuration = 150,
            totalDuration = 200,
            connectionReused = true
        )

        // Should not throw any exception
        panel.updateProfilingData(data)
        assertNotNull("Panel should be created", panel)
    }

    /**
     * Test: Update panel with HTTP (no SSL) data
     */
    @Test
    fun testUpdateWithHTTPData() {
        val panel = ProfilingTimelinePanel()
        val data = HttpProfilingData(
            dnsDuration = 30,
            tcpDuration = 60,
            sslDuration = null,
            ttfbDuration = 200,
            totalDuration = 320,
            connectionReused = false
        )

        // Should not throw any exception
        panel.updateProfilingData(data)
        assertNotNull("Panel should be created", panel)
    }

    /**
     * Test: Clear panel
     */
    @Test
    fun testClearPanel() {
        val panel = ProfilingTimelinePanel()
        val data = HttpProfilingData(
            dnsDuration = 50,
            tcpDuration = 80,
            sslDuration = 120,
            ttfbDuration = 300,
            totalDuration = 450,
            connectionReused = false
        )

        panel.updateProfilingData(data)
        panel.clear()

        // Should not throw any exception
        assertNotNull("Panel should still exist", panel)
    }

    /**
     * Test: Multiple updates
     */
    @Test
    fun testMultipleUpdates() {
        val panel = ProfilingTimelinePanel()

        val data1 = HttpProfilingData(
            dnsDuration = 50,
            tcpDuration = 80,
            sslDuration = 120,
            ttfbDuration = 300,
            totalDuration = 450,
            connectionReused = false
        )

        val data2 = HttpProfilingData(
            dnsDuration = null,
            tcpDuration = null,
            sslDuration = null,
            ttfbDuration = 150,
            totalDuration = 200,
            connectionReused = true
        )

        // Should handle multiple updates without issues
        panel.updateProfilingData(data1)
        panel.updateProfilingData(data2)
        panel.updateProfilingData(HttpProfilingData.Empty)

        assertNotNull("Panel should still exist", panel)
    }

    /**
     * Test: Update with empty data
     */
    @Test
    fun testUpdateWithEmptyData() {
        val panel = ProfilingTimelinePanel()

        // Should handle empty data gracefully
        panel.updateProfilingData(HttpProfilingData.Empty)
        assertNotNull("Panel should be created", panel)
    }

    /**
     * Test: Data with very small durations
     */
    @Test
    fun testUpdateWithSmallDurations() {
        val panel = ProfilingTimelinePanel()
        val data = HttpProfilingData(
            dnsDuration = 1,
            tcpDuration = 2,
            sslDuration = 3,
            ttfbDuration = 5,
            totalDuration = 10,
            connectionReused = false
        )

        // Should handle very small durations
        panel.updateProfilingData(data)
        assertNotNull("Panel should be created", panel)
    }

    /**
     * Test: Data with very large durations (seconds)
     */
    @Test
    fun testUpdateWithLargeDurations() {
        val panel = ProfilingTimelinePanel()
        val data = HttpProfilingData(
            dnsDuration = 1500,
            tcpDuration = 2000,
            sslDuration = 2500,
            ttfbDuration = 5000,
            totalDuration = 8000,
            connectionReused = false
        )

        // Should handle large durations (in seconds)
        panel.updateProfilingData(data)
        assertNotNull("Panel should be created", panel)
    }
}

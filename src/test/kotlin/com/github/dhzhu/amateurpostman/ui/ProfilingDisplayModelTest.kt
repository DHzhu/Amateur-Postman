package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpProfilingData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for ProfilingDisplayModel.
 * Tests the conversion from HttpProfilingData to UI display format.
 */
class ProfilingDisplayModelTest {

    /**
     * Test: Full profiling data with all metrics available (fresh connection with SSL)
     */
    @Test
    fun testFromProfilingDataWithFullMetrics() {
        val data = HttpProfilingData(
            dnsDuration = 50,
            tcpDuration = 80,
            sslDuration = 120,
            ttfbDuration = 300,
            totalDuration = 450,
            connectionReused = false
        )

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)

        assertTrue(displayModel.hasProfilingData, "Should have profiling data")
        assertEquals("50 ms", displayModel.dnsDuration)
        assertEquals("80 ms", displayModel.tcpDuration)
        assertEquals("120 ms", displayModel.sslDuration)
        assertEquals("300 ms", displayModel.ttfbDuration)
        assertEquals("450 ms", displayModel.totalDuration)
        assertEquals("No", displayModel.connectionReused)
    }

    /**
     * Test: Connection reuse scenario (DNS, TCP, SSL are null)
     */
    @Test
    fun testFromProfilingDataWithConnectionReuse() {
        val data = HttpProfilingData(
            dnsDuration = null,
            tcpDuration = null,
            sslDuration = null,
            ttfbDuration = 150,
            totalDuration = 200,
            connectionReused = true
        )

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)

        assertTrue(displayModel.hasProfilingData, "Should have profiling data")
        assertEquals("Connection Reused", displayModel.dnsDuration)
        assertEquals("Connection Reused", displayModel.tcpDuration)
        assertEquals("Connection Reused", displayModel.sslDuration)
        assertEquals("150 ms", displayModel.ttfbDuration)
        assertEquals("200 ms", displayModel.totalDuration)
        assertEquals("Yes", displayModel.connectionReused)
    }

    /**
     * Test: HTTP connection without SSL
     */
    @Test
    fun testFromProfilingDataWithHTTP() {
        val data = HttpProfilingData(
            dnsDuration = 30,
            tcpDuration = 60,
            sslDuration = null,
            ttfbDuration = 200,
            totalDuration = 320,
            connectionReused = false
        )

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)

        assertTrue(displayModel.hasProfilingData, "Should have profiling data")
        assertEquals("30 ms", displayModel.dnsDuration)
        assertEquals("60 ms", displayModel.tcpDuration)
        assertEquals("N/A (HTTP)", displayModel.sslDuration)
        assertEquals("200 ms", displayModel.ttfbDuration)
        assertEquals("320 ms", displayModel.totalDuration)
        assertEquals("No", displayModel.connectionReused)
    }

    /**
     * Test: Empty profiling data
     */
    @Test
    fun testFromProfilingDataWithEmpty() {
        val data = HttpProfilingData.Empty

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)

        assertFalse(displayModel.hasProfilingData, "Should not have profiling data")
        assertEquals("N/A", displayModel.dnsDuration)
        assertEquals("N/A", displayModel.tcpDuration)
        assertEquals("N/A", displayModel.sslDuration)
        assertEquals("N/A", displayModel.ttfbDuration)
        assertEquals("N/A", displayModel.totalDuration)
        assertEquals("N/A", displayModel.connectionReused)
    }

    /**
     * Test: Duration formatting with seconds (>= 1000ms)
     */
    @Test
    fun testDurationFormattingWithSeconds() {
        val data = HttpProfilingData(
            dnsDuration = 1500,
            tcpDuration = 2300,
            sslDuration = 120,
            ttfbDuration = 3500,
            totalDuration = 5200,
            connectionReused = false
        )

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)

        assertTrue(displayModel.hasProfilingData, "Should have profiling data")
        assertEquals("1.50 s", displayModel.dnsDuration)
        assertEquals("2.30 s", displayModel.tcpDuration)
        assertEquals("120 ms", displayModel.sslDuration)
        assertEquals("3.50 s", displayModel.ttfbDuration)
        assertEquals("5.20 s", displayModel.totalDuration)
    }

    /**
     * Test: Summary generation with full data
     */
    @Test
    fun testToSummaryWithFullData() {
        val data = HttpProfilingData(
            dnsDuration = 50,
            tcpDuration = 80,
            sslDuration = 120,
            ttfbDuration = 300,
            totalDuration = 450,
            connectionReused = false
        )

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)
        val summary = displayModel.toSummary()

        assertTrue(summary.contains("Total Time: 450 ms"), "Summary should contain total time")
        assertTrue(summary.contains("DNS: 50 ms"), "Summary should contain DNS")
        assertTrue(summary.contains("TCP: 80 ms"), "Summary should contain TCP")
        assertTrue(summary.contains("SSL: 120 ms"), "Summary should contain SSL")
        assertTrue(summary.contains("TTFB: 300 ms"), "Summary should contain TTFB")
        assertTrue(summary.contains("Connection Reused: No"), "Summary should show connection not reused")
    }

    /**
     * Test: Summary generation with connection reuse
     */
    @Test
    fun testToSummaryWithConnectionReuse() {
        val data = HttpProfilingData(
            dnsDuration = null,
            tcpDuration = null,
            sslDuration = null,
            ttfbDuration = 150,
            totalDuration = 200,
            connectionReused = true
        )

        val displayModel = ProfilingDisplayModel.fromProfilingData(data)
        val summary = displayModel.toSummary()

        assertTrue(summary.contains("Total Time: 200 ms"), "Summary should contain total time")
        assertFalse(summary.contains("DNS:"), "Summary should not contain DNS (reused)")
        assertFalse(summary.contains("TCP:"), "Summary should not contain TCP (reused)")
        assertFalse(summary.contains("SSL:"), "Summary should not contain SSL (reused)")
        assertTrue(summary.contains("TTFB: 150 ms"), "Summary should contain TTFB")
        assertTrue(summary.contains("Connection Reused: Yes"), "Summary should show connection reused")
    }

    /**
     * Test: Summary with empty data
     */
    @Test
    fun testToSummaryWithEmptyData() {
        val displayModel = ProfilingDisplayModel.fromProfilingData(HttpProfilingData.Empty)
        val summary = displayModel.toSummary()

        assertEquals("No profiling data available", summary)
    }
}

package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpProfilingData

/**
 * Display model for profiling data in the UI.
 * Converts raw profiling data into formatted strings for presentation.
 */
data class ProfilingDisplayModel(
    val totalDuration: String,
    val dnsDuration: String,
    val tcpDuration: String,
    val sslDuration: String,
    val ttfbDuration: String,
    val connectionReused: String,
    val hasProfilingData: Boolean
) {
    companion object {
        /**
         * Create display model from HttpProfilingData.
         * Returns a model with empty values if profiling data is not available.
         */
        fun fromProfilingData(data: HttpProfilingData): ProfilingDisplayModel {
            if (data == HttpProfilingData.Empty) {
                return ProfilingDisplayModel(
                    totalDuration = "N/A",
                    dnsDuration = "N/A",
                    tcpDuration = "N/A",
                    sslDuration = "N/A",
                    ttfbDuration = "N/A",
                    connectionReused = "N/A",
                    hasProfilingData = false
                )
            }

            // For connection reuse, show "Connection Reused" for null values
            // For fresh connections with null SSL, show "N/A (HTTP)"
            val connectionReusedPlaceholder = "Connection Reused"
            val httpPlaceholder = "N/A (HTTP)"

            return ProfilingDisplayModel(
                totalDuration = formatDuration(data.totalDuration),
                dnsDuration = formatNullableDuration(data.dnsDuration, connectionReusedPlaceholder),
                tcpDuration = formatNullableDuration(data.tcpDuration, connectionReusedPlaceholder),
                sslDuration = when {
                    data.connectionReused -> connectionReusedPlaceholder
                    data.sslDuration != null -> formatDuration(data.sslDuration)
                    else -> httpPlaceholder
                },
                ttfbDuration = formatDuration(data.ttfbDuration ?: 0),
                connectionReused = if (data.connectionReused) "Yes" else "No",
                hasProfilingData = true
            )
        }

        private fun formatDuration(ms: Long): String {
            return if (ms >= 1000) {
                String.format("%.2f s", ms / 1000.0)
            } else {
                "${ms} ms"
            }
        }

        private fun formatNullableDuration(ms: Long?, placeholder: String): String {
            return if (ms != null) formatDuration(ms) else placeholder
        }
    }

    /**
     * Generate a summary text for quick overview.
     */
    fun toSummary(): String {
        if (!hasProfilingData) {
            return "No profiling data available"
        }

        return buildString {
            appendLine("Total Time: $totalDuration")
            if (dnsDuration != "Connection Reused") {
                appendLine("  DNS: $dnsDuration")
            }
            if (tcpDuration != "Connection Reused") {
                appendLine("  TCP: $tcpDuration")
            }
            // Only show SSL if it's a valid value (not connection reuse and not HTTP)
            if (sslDuration != "N/A (HTTP)" && sslDuration != "Connection Reused") {
                appendLine("  SSL: $sslDuration")
            }
            appendLine("  TTFB: $ttfbDuration")
            append("Connection Reused: $connectionReused")
        }
    }
}

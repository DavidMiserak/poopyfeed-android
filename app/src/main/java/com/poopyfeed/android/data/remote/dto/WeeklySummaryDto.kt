package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Weekly (7-day) activity summary for pediatrician report.
 * Endpoint: GET /api/v1/analytics/children/{id}/weekly-summary/
 */
@Serializable
data class WeeklySummaryResponse(
    @SerialName("child_id")
    val childId: Int,
    val period: String,
    val feedings: WeeklySummaryFeedings,
    val diapers: WeeklySummaryDiapers,
    val sleep: WeeklySummarySleep,
    @SerialName("last_updated")
    val lastUpdated: String,
)

@Serializable
data class WeeklySummaryFeedings(
    val count: Int,
    @SerialName("total_oz")
    val totalOz: Double = 0.0,
    val bottle: Int = 0,
    val breast: Int = 0,
    @SerialName("avg_duration")
    val avgDuration: Double? = null,
)

@Serializable
data class WeeklySummaryDiapers(
    val count: Int,
    val wet: Int = 0,
    val dirty: Int = 0,
    val both: Int = 0,
)

@Serializable
data class WeeklySummarySleep(
    val naps: Int,
    @SerialName("total_minutes")
    val totalMinutes: Int = 0,
    @SerialName("avg_duration")
    val avgDuration: Int = 0,
)

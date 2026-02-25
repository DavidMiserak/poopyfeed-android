package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Today's activity summary for a child.
 * Endpoint: GET /api/v1/analytics/children/{id}/today-summary/
 */
@Serializable
data class TodaySummaryResponse(
    @SerialName("child_id")
    val childId: Int,
    val period: String,
    val feedings: TodaySummaryFeedings,
    val diapers: TodaySummaryDiapers,
    val sleep: TodaySummarySleep,
    @SerialName("last_updated")
    val lastUpdated: String,
)

@Serializable
data class TodaySummaryFeedings(
    val count: Int,
    @SerialName("total_oz")
    val totalOz: Double = 0.0,
    val bottle: Int = 0,
    val breast: Int = 0,
)

@Serializable
data class TodaySummaryDiapers(
    val count: Int,
    val wet: Int = 0,
    val dirty: Int = 0,
    val both: Int = 0,
)

@Serializable
data class TodaySummarySleep(
    val naps: Int,
    @SerialName("total_minutes")
    val totalMinutes: Int = 0,
    @SerialName("avg_duration")
    val avgDuration: Int = 0,
)

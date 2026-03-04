package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pattern-based alerts for a child (feeding interval, nap wake window).
 * Endpoint: GET /api/v1/analytics/children/{id}/pattern-alerts/
 */
@Serializable
data class PatternAlertsResponse(
    @SerialName("child_id")
    val childId: Int,
    val feeding: PatternAlertFeeding,
    val nap: PatternAlertNap,
)

@Serializable
data class PatternAlertFeeding(
    val alert: Boolean,
    val message: String? = null,
    @SerialName("avg_interval_minutes")
    val avgIntervalMinutes: Int? = null,
    @SerialName("minutes_since_last")
    val minutesSinceLast: Int? = null,
    @SerialName("last_fed_at")
    val lastFedAt: String? = null,
    @SerialName("data_points")
    val dataPoints: Int = 0,
)

@Serializable
data class PatternAlertNap(
    val alert: Boolean,
    val message: String? = null,
    @SerialName("avg_wake_window_minutes")
    val avgWakeWindowMinutes: Int? = null,
    @SerialName("minutes_awake")
    val minutesAwake: Int? = null,
    @SerialName("last_nap_ended_at")
    val lastNapEndedAt: String? = null,
    @SerialName("data_points")
    val dataPoints: Int = 0,
)

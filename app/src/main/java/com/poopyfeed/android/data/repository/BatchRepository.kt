package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.BatchApi
import com.poopyfeed.android.data.remote.dto.BatchErrorResponse
import com.poopyfeed.android.data.remote.dto.BatchResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

private inline fun <reified T> Json.encodeRoot(value: T): String = encodeToString(serializer(), value)

/**
 * Payload for a single event in a batch (catch-up) request.
 * Backend expects type + data object per event.
 */
sealed class CatchUpBatchEvent {
    abstract val type: String

    abstract fun toDataJson(): JsonObject

    data class Feeding(
        val fedAt: String,
        val feedingType: String,
        val amountOz: Double? = null,
        val durationMinutes: Int? = null,
        val side: String? = null,
    ) : CatchUpBatchEvent() {
        override val type: String = "feeding"

        override fun toDataJson(): JsonObject =
            buildJsonObject {
                put("fed_at", fedAt)
                put("feeding_type", feedingType)
                amountOz?.let { put("amount_oz", it) }
                durationMinutes?.let { put("duration_minutes", it) }
                side?.let { put("side", it) }
            }
    }

    data class Diaper(
        val changedAt: String,
        val changeType: String,
    ) : CatchUpBatchEvent() {
        override val type: String = "diaper"

        override fun toDataJson(): JsonObject =
            buildJsonObject {
                put("changed_at", changedAt)
                put("change_type", changeType)
            }
    }

    data class Nap(
        val nappedAt: String,
    ) : CatchUpBatchEvent() {
        override val type: String = "nap"

        override fun toDataJson(): JsonObject =
            buildJsonObject {
                put("napped_at", nappedAt)
            }
    }
}

@Singleton
class BatchRepository
    @Inject
    constructor(
        private val batchApi: BatchApi,
        private val json: Json,
    ) {
        suspend fun createBatch(
            childId: Int,
            events: List<CatchUpBatchEvent>,
        ): Result<BatchResponse> {
            if (events.isEmpty()) {
                return Result.failure(Exception("At least one event is required."))
            }
            if (events.size > 20) {
                return Result.failure(Exception("Maximum 20 events per batch."))
            }
            return try {
                val requestBody = buildRequest(events)
                val body = requestBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                val response = batchApi.createBatch(childId, body)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val message = parseBatchError(errorBody) ?: ChildrenRepository.parseErrorBody(errorBody)
                    return Result.failure(Exception(message))
                }
                val batchResponse = response.body() ?: return Result.failure(Exception("Empty batch response"))
                Result.success(batchResponse)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        private fun buildRequest(events: List<CatchUpBatchEvent>): String {
            val eventsArray =
                buildJsonArray {
                    events.forEach { e ->
                        add(
                            buildJsonObject {
                                put("type", e.type)
                                putJsonObject("data") {
                                    e.toDataJson().forEach { (k, v) -> put(k, v) }
                                }
                            },
                        )
                    }
                }
            val root = buildJsonObject { put("events", eventsArray) }
            return json.encodeRoot(root)
        }

        private fun parseBatchError(errorBody: String?): String? {
            if (errorBody.isNullOrBlank()) return null
            return try {
                val err = json.decodeFromString<BatchErrorResponse>(errorBody)
                if (err.errors.isEmpty()) {
                    null
                } else {
                    err.errors.joinToString(". ") { ev ->
                        val msgs = ev.errors.values.flatten()
                        "Event ${ev.index + 1} (${ev.type}): ${msgs.joinToString("; ")}"
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

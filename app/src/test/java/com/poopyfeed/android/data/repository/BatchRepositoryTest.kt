package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.BatchApi
import com.poopyfeed.android.data.remote.dto.BatchCreatedItem
import com.poopyfeed.android.data.remote.dto.BatchResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

class BatchRepositoryTest {
    private lateinit var batchApi: BatchApi
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    private lateinit var repository: BatchRepository

    @Before
    fun setup() {
        batchApi = mock()
        repository = BatchRepository(batchApi, json)
    }

    @Test
    fun `createBatch with non-empty list returns success`() =
        runTest {
            val events =
                listOf(
                    CatchUpBatchEvent.Feeding(
                        fedAt = "2024-01-15T12:00:00Z",
                        feedingType = "bottle",
                        amountOz = 4.0,
                    ),
                )
            val batchResponse =
                BatchResponse(
                    created = listOf(BatchCreatedItem(type = "feeding", id = 1)),
                    count = 1,
                )
            whenever(batchApi.createBatch(any(), any())).thenReturn(Response.success(batchResponse))

            val result = repository.createBatch(1, events)

            assertTrue(result.isSuccess)
            assertEquals(batchResponse, result.getOrNull())
        }

    @Test
    fun `createBatch with empty list returns failure`() =
        runTest {
            val result = repository.createBatch(1, emptyList())

            assertTrue(result.isFailure)
            assertEquals(
                "At least one event is required.",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `createBatch with more than 20 events returns failure`() =
        runTest {
            val events =
                List(21) {
                    CatchUpBatchEvent.Diaper(
                        changedAt = "2024-01-15T12:00:00Z",
                        changeType = "wet",
                    )
                }
            val result = repository.createBatch(1, events)

            assertTrue(result.isFailure)
            assertEquals(
                "Maximum 20 events per batch.",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `createBatch 400 with batch errors returns parsed message`() =
        runTest {
            val events =
                listOf(
                    CatchUpBatchEvent.Feeding(
                        fedAt = "2024-01-15T12:00:00Z",
                        feedingType = "bottle",
                        amountOz = 4.0,
                    ),
                )
            val errorBody =
                """{"errors":[{"index":0,"type":"feeding","errors":{"fed_at":["This field is required."]}}]}"""
                    .toResponseBody("application/json".toMediaType())
            whenever(batchApi.createBatch(any(), any())).thenReturn(Response.error(400, errorBody))

            val result = repository.createBatch(1, events)

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()?.message?.contains("Event 1 (feeding)") == true,
            )
            assertTrue(
                result.exceptionOrNull()?.message?.contains("This field is required") == true,
            )
        }

    @Test
    fun `createBatch 4xx generic returns parseErrorBody message`() =
        runTest {
            val events =
                listOf(
                    CatchUpBatchEvent.Nap(nappedAt = "2024-01-15T13:00:00Z"),
                )
            val errorBody =
                """{"detail":"Forbidden"}"""
                    .toResponseBody("application/json".toMediaType())
            whenever(batchApi.createBatch(any(), any())).thenReturn(Response.error(403, errorBody))

            val result = repository.createBatch(1, events)

            assertTrue(result.isFailure)
            assertEquals("Forbidden", result.exceptionOrNull()?.message)
        }
}

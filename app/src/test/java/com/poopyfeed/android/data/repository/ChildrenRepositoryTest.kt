package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.ChildrenApi
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.ChildrenResponse
import kotlinx.coroutines.test.runTest
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

class ChildrenRepositoryTest {
    private lateinit var childrenApi: ChildrenApi
    private lateinit var repository: ChildrenRepository

    private val mockChild =
        Child(
            id = 1,
            name = "Baby",
            dateOfBirth = "2024-01-15",
            gender = "F",
            userRole = "owner",
            canEdit = true,
            canManageSharing = true,
            createdAt = "2024-01-15T10:00:00Z",
            updatedAt = "2024-01-15T10:00:00Z",
            lastDiaperChange = null,
            lastNap = null,
            lastFeeding = null,
        )

    @Before
    fun setup() {
        childrenApi = mock()
        repository = ChildrenRepository(childrenApi)
    }

    @Test
    fun `getChildren success returns list`() =
        runTest {
            val response = Response.success(ChildrenResponse(results = listOf(mockChild)))
            whenever(childrenApi.getChildren()).thenReturn(response)

            val result = repository.getChildren()

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()!!.size)
            assertEquals("Baby", result.getOrNull()!![0].name)
        }

    @Test
    fun `getChildren API error returns failure`() =
        runTest {
            val errorBody =
                """{"detail":"Forbidden"}"""
                    .toResponseBody("application/json".toMediaType())
            whenever(childrenApi.getChildren()).thenReturn(Response.error(403, errorBody))

            val result = repository.getChildren()

            assertTrue(result.isFailure)
            assertEquals("Forbidden", result.exceptionOrNull()?.message)
        }

    @Test
    fun `getChildren empty body returns failure`() =
        runTest {
            whenever(childrenApi.getChildren()).thenReturn(Response.success(null))

            val result = repository.getChildren()

            assertTrue(result.isFailure)
            assertEquals("Empty children response", result.exceptionOrNull()?.message)
        }

    @Test
    fun `getChildren network exception returns failure`() =
        runTest {
            whenever(childrenApi.getChildren()).thenThrow(RuntimeException("Unable to resolve host"))

            val result = repository.getChildren()

            assertTrue(result.isFailure)
            assertEquals("No internet connection. Please check your network.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `getChild success returns child`() =
        runTest {
            whenever(childrenApi.getChild(1)).thenReturn(Response.success(mockChild))

            val result = repository.getChild(1)

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()!!.id)
            assertEquals("Baby", result.getOrNull()!!.name)
        }

    @Test
    fun `getChild 404 returns failure`() =
        runTest {
            val errorBody = """{"detail":"Not found"}""".toResponseBody("application/json".toMediaType())
            whenever(childrenApi.getChild(999)).thenReturn(Response.error(404, errorBody))

            val result = repository.getChild(999)

            assertTrue(result.isFailure)
            assertEquals("Not found", result.exceptionOrNull()?.message)
        }

    @Test
    fun `createChild success returns child`() =
        runTest {
            whenever(childrenApi.createChild(any())).thenReturn(Response.success(mockChild))

            val result = repository.createChild("Baby", "2024-01-15", "F")

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()!!.id)
        }

    @Test
    fun `createChild validation error returns failure`() =
        runTest {
            val errorBody =
                """{"name":["This field may not be blank."]}"""
                    .toResponseBody("application/json".toMediaType())
            whenever(childrenApi.createChild(any())).thenReturn(Response.error(400, errorBody))

            val result = repository.createChild("", "2024-01-15", null)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("This field may not be blank."))
        }

    @Test
    fun `updateChild success returns child`() =
        runTest {
            whenever(childrenApi.updateChild(any(), any())).thenReturn(Response.success(mockChild))

            val result = repository.updateChild(1, name = "Updated")

            assertTrue(result.isSuccess)
            assertEquals("Baby", result.getOrNull()!!.name)
        }

    @Test
    fun `updateChild 404 returns failure`() =
        runTest {
            val errorBody = """{"detail":"Not found"}""".toResponseBody("application/json".toMediaType())
            whenever(childrenApi.updateChild(any(), any())).thenReturn(Response.error(404, errorBody))

            val result = repository.updateChild(999, name = "X")

            assertTrue(result.isFailure)
        }

    @Test
    fun `deleteChild success returns Unit`() =
        runTest {
            whenever(childrenApi.deleteChild(1)).thenReturn(Response.success(Unit))

            val result = repository.deleteChild(1)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `deleteChild 404 returns failure`() =
        runTest {
            val errorBody = """{"detail":"Not found"}""".toResponseBody("application/json".toMediaType())
            whenever(childrenApi.deleteChild(999)).thenReturn(Response.error(404, errorBody))

            val result = repository.deleteChild(999)

            assertTrue(result.isFailure)
        }
}

package net.poopyfeed.pf.data.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TrackingRepositoriesTest {

    private lateinit var apiService: PoopyFeedApiService

    @Before
    fun setup() {
        apiService = io.mockk.mockk()
    }

    @Test
    fun `FeedingsRepository listFeedings emits Loading then Success`() = runTest {
        val repository = FeedingsRepository(apiService)

        val feeding = Feeding(
            id = 1,
            child = 1,
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T12:00:00Z",
            created_at = "2024-01-15T12:00:00Z",
            updated_at = "2024-01-15T12:00:00Z"
        )

        val response = PaginatedResponse(
            count = 1,
            results = listOf(feeding)
        )

        io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 1) } returns response

        val results = repository.listFeedings(childId = 1, page = 1).toList()

        assertEquals(2, results.size)
        assertIs<ApiResult.Loading<*>>(results[0])
        assertIs<ApiResult.Success<*>>(results[1])
    }

    @Test
    fun `FeedingsRepository createFeeding network error returns Error`() = runTest {
        val repository = FeedingsRepository(apiService)

        val request = CreateFeedingRequest(
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T12:00:00Z"
        )

        io.mockk.coEvery {
            apiService.createFeeding(1, request)
        } throws IOException("Network down")

        val result = repository.createFeeding(1, request)

        assertIs<ApiResult.Error<Feeding>>(result)
        assertIs<ApiError.NetworkError>(result.error)
    }

    @Test
    fun `DiapersRepository listDiapers emits Loading then Error on http 500`() = runTest {
        val repository = DiapersRepository(apiService)

        val errorResponse = retrofit2.Response.error<PaginatedResponse<Diaper>>(
            500,
            "Server error".toResponseBody(null)
        )

        io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } throws retrofit2.HttpException(
            errorResponse
        )

        val results = repository.listDiapers(childId = 1, page = 1).toList()

        assertEquals(2, results.size)
        assertIs<ApiResult.Loading<*>>(results[0])
        assertIs<ApiResult.Error<*>>(results[1])
        val error = (results[1] as ApiResult.Error).error
        assertIs<ApiError.HttpError>(error)
        assertEquals(500, error.statusCode)
    }

    @Test
    fun `NapsRepository createNap success returns Success`() = runTest {
        val repository = NapsRepository(apiService)

        val request = CreateNapRequest(
            start_time = "2024-01-15T13:00:00Z",
            end_time = null
        )

        val nap = Nap(
            id = 1,
            child = 1,
            start_time = "2024-01-15T13:00:00Z",
            end_time = null,
            created_at = "2024-01-15T13:00:00Z",
            updated_at = "2024-01-15T13:00:00Z"
        )

        io.mockk.coEvery { apiService.createNap(1, request) } returns nap

        val result = repository.createNap(1, request)

        assertIs<ApiResult.Success<Nap>>(result)
        assertEquals(nap, result.data)
    }

    @Test
    fun `SharingRepository listShares success returns list`() = runTest {
        val repository = SharingRepository(apiService)

        val share = ChildShare(
            id = 1,
            child = 1,
            shared_with_user = "friend@example.com",
            role = "co-parent",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z"
        )

        io.mockk.coEvery { apiService.listShares(1) } returns listOf(share)

        val result = repository.listShares(1)

        assertIs<ApiResult.Success<List<ChildShare>>>(result)
        assertEquals(1, result.data.size)
        assertEquals("friend@example.com", result.data.first().shared_with_user)
    }

    @Test
    fun `SharingRepository acceptInvite http error returns Error`() = runTest {
        val repository = SharingRepository(apiService)

        val errorResponse = retrofit2.Response.error<ShareInvite>(
            400,
            "Bad request".toResponseBody(null)
        )

        io.mockk.coEvery { apiService.acceptInvite(42) } throws retrofit2.HttpException(
            errorResponse
        )

        val result = repository.acceptInvite(42)

        assertIs<ApiResult.Error<ShareInvite>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(400, (result.error as ApiError.HttpError).statusCode)
    }
}

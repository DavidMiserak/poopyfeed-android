package net.poopyfeed.pf.data.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NotificationsPagingSourceTest {

    private val mockApiService = mockk<net.poopyfeed.pf.data.api.PoopyFeedApiService>()
    private lateinit var pagingSource: NotificationsPagingSource

    @Before
    fun setup() {
        pagingSource = NotificationsPagingSource(mockApiService)
    }

    @Test
    fun `load first page returns correct data and keys`() = runTest {
        val mockNotification1 = TestFixtures.mockNotification(id = 1)
        val mockNotification2 = TestFixtures.mockNotification(id = 2)
        val mockResponse = TestFixtures.mockPaginatedNotificationsResponse(
            results = listOf(mockNotification1, mockNotification2),
            count = 40,
            next = "http://example.com/page2",
            previous = null
        )

        coEvery { mockApiService.listNotifications(1, 20) } returns mockResponse

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(2, page.data.size)
        assertEquals(mockNotification1, page.data[0])
        assertEquals(mockNotification2, page.data[1])
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun `load middle page returns correct prev and next keys`() = runTest {
        val mockNotification = TestFixtures.mockNotification(id = 20)
        val mockResponse = TestFixtures.mockPaginatedNotificationsResponse(
            results = listOf(mockNotification),
            count = 40,
            next = "http://example.com/page3",
            previous = "http://example.com/page1"
        )

        coEvery { mockApiService.listNotifications(2, 20) } returns mockResponse

        val result = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = 2,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals(1, page.prevKey)
        assertEquals(3, page.nextKey)
    }

    @Test
    fun `load last page returns null nextKey`() = runTest {
        val mockNotification = TestFixtures.mockNotification(id = 35)
        val mockResponse = TestFixtures.mockPaginatedNotificationsResponse(
            results = listOf(mockNotification),
            count = 40,
            next = null,
            previous = "http://example.com/page1"
        )

        coEvery { mockApiService.listNotifications(2, 20) } returns mockResponse

        val result = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = 2,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals(1, page.prevKey)
        assertNull(page.nextKey)
    }

    @Test
    fun `load with empty results`() = runTest {
        val mockResponse = TestFixtures.mockPaginatedNotificationsResponse(
            results = emptyList(),
            count = 0,
            next = null,
            previous = null
        )

        coEvery { mockApiService.listNotifications(1, 20) } returns mockResponse

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val page = result as PagingSource.LoadResult.Page
        assertEquals(0, page.data.size)
        assertNull(page.prevKey)
        assertNull(page.nextKey)
    }

    @Test
    fun `load network error returns Error result`() = runTest {
        coEvery { mockApiService.listNotifications(1, 20) } throws
            java.io.IOException("Network error")

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val errorResult = result as PagingSource.LoadResult.Error
        assertEquals(true, errorResult.throwable is java.io.IOException)
    }

    @Test
    fun `load http error returns Error result`() = runTest {
        val errorResponse = retrofit2.Response.error<Any>(
            401,
            "Unauthorized".toResponseBody(null)
        )

        coEvery { mockApiService.listNotifications(1, 20) } throws
            retrofit2.HttpException(errorResponse)

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val errorResult = result as PagingSource.LoadResult.Error
        assertEquals(true, errorResult.throwable is retrofit2.HttpException)
    }

    @Test
    fun `getRefreshKey returns correct page key`() {
        val mockNotification1 = TestFixtures.mockNotification(id = 1)
        val mockNotification2 = TestFixtures.mockNotification(id = 2)

        val page = PagingSource.LoadResult.Page(
            data = listOf(mockNotification1, mockNotification2),
            prevKey = 1,
            nextKey = 3
        )

        val state = PagingState(
            pages = listOf(page),
            anchorPosition = 0,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        val refreshKey = pagingSource.getRefreshKey(state)
        assertEquals(2, refreshKey)
    }
}

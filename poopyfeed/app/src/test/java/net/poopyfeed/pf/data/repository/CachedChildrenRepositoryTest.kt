package net.poopyfeed.pf.data.repository

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.ChildDao
import net.poopyfeed.pf.data.db.ChildEntity
import net.poopyfeed.pf.data.models.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedChildrenRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var childDao: ChildDao
  private lateinit var repository: CachedChildrenRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    childDao = io.mockk.mockk()
    repository = CachedChildrenRepository(apiService, childDao, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listChildrenCached emits Success with empty list when cache empty`() = runTest {
    io.mockk.coEvery { childDao.getAllChildrenFlow() } returns flowOf(emptyList())

    val results = repository.listChildrenCached().toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Child>>>(results[0])
    assertEquals(0, (results[0] as ApiResult.Success).data.size)
  }

  @Test
  fun `listChildrenCached emits Success when cache has data`() = runTest {
    val entities =
        listOf(
            ChildEntity(
                id = 1,
                name = "Baby Alice",
                date_of_birth = "2024-01-15",
                gender = "F",
                user_role = "owner",
                created_at = "2024-01-15T10:00:00Z",
                updated_at = "2024-01-15T10:00:00Z"))
    io.mockk.coEvery { childDao.getAllChildrenFlow() } returns flowOf(entities)

    val results = repository.listChildrenCached().toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Child>>>(results[0])
    assertEquals("Baby Alice", (results[0] as ApiResult.Success).data.first().name)
  }

  @Test
  fun `refreshChildren success upserts and returns Success`() = runTest {
    val child = TestFixtures.mockChild()
    val response = PaginatedResponse(count = 1, results = listOf(child))
    io.mockk.coEvery { apiService.listChildren(page = 1) } returns response
    io.mockk.coEvery { childDao.upsertChildren(any()) } returns Unit

    val result = repository.refreshChildren()

    assertIs<ApiResult.Success<List<Child>>>(result)
    assertEquals(1, result.data.size)
    assertEquals("Baby Alice", result.data.first().name)
  }

  @Test
  fun `refreshChildren fetches all pages when next is non-null`() = runTest {
    val child1 = TestFixtures.mockChild(id = 1, name = "Alice")
    val child2 =
        TestFixtures.mockChild(
            id = 2,
            name = "Bob",
            date_of_birth = "2024-06-01",
            gender = "M",
            created_at = "2024-06-01T10:00:00Z",
            updated_at = "2024-06-01T10:00:00Z")
    io.mockk.coEvery { apiService.listChildren(page = 1) } returns
        PaginatedResponse(count = 2, next = "http://api/children/?page=2", results = listOf(child1))
    io.mockk.coEvery { apiService.listChildren(page = 2) } returns
        PaginatedResponse(count = 2, next = null, results = listOf(child2))
    io.mockk.coEvery { childDao.upsertChildren(any()) } returns Unit

    val result = repository.refreshChildren()

    assertIs<ApiResult.Success<List<Child>>>(result)
    assertEquals(2, result.data.size)
    assertEquals("Alice", result.data[0].name)
    assertEquals("Bob", result.data[1].name)
    io.mockk.coVerify { apiService.listChildren(1) }
    io.mockk.coVerify { apiService.listChildren(2) }
  }

  @Test
  fun `hasSyncedFlow emits false initially and true after successful refresh`() = runTest {
    io.mockk.coEvery { childDao.getAllChildrenFlow() } returns flowOf(emptyList())
    val syncedValues = mutableListOf<Boolean>()
    val job = launch { repository.hasSyncedFlow.collect { syncedValues.add(it) } }
    yield()
    assertEquals(listOf(false), syncedValues)

    val child = TestFixtures.mockChild(name = "Baby")
    io.mockk.coEvery { apiService.listChildren(page = 1) } returns
        PaginatedResponse(count = 1, results = listOf(child))
    io.mockk.coEvery { childDao.upsertChildren(any()) } returns Unit
    repository.refreshChildren()
    yield()
    job.cancel()
    assertEquals(listOf(false, true), syncedValues)
  }

  @Test
  fun `hasSyncedFlow emits false after clearCache`() = runTest {
    val child = TestFixtures.mockChild(name = "Baby")
    io.mockk.coEvery { apiService.listChildren(page = 1) } returns
        PaginatedResponse(count = 1, results = listOf(child))
    io.mockk.coEvery { childDao.upsertChildren(any()) } returns Unit
    io.mockk.coEvery { childDao.clearAll() } returns Unit
    repository.refreshChildren()
    assertEquals(true, repository.hasSyncedFlow.first())
    repository.clearCache()
    assertEquals(false, repository.hasSyncedFlow.first())
  }

  @Test
  fun `refreshChildren network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.listChildren(any()) } throws IOException("Network down")

    val result = repository.refreshChildren()

    assertIs<ApiResult.Error<List<Child>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `getChildCached maps entity to Child`() = runTest {
    val entity =
        ChildEntity(
            id = 1,
            name = "Baby Alice",
            date_of_birth = "2024-01-15",
            gender = "F",
            user_role = "owner",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z")
    io.mockk.every { childDao.getChildFlow(1) } returns flowOf(entity)

    val results = repository.getChildCached(1).toList()

    assertEquals(1, results.size)
    assertEquals("Baby Alice", results[0]?.name)
  }

  @Test
  fun `getChildCached emits null when not found`() = runTest {
    io.mockk.every { childDao.getChildFlow(999) } returns flowOf(null)

    val results = repository.getChildCached(999).toList()

    assertEquals(1, results.size)
    assertEquals(null, results[0])
  }

  @Test
  fun `createChild success upserts and returns Success`() = runTest {
    val request = CreateChildRequest("Baby Bob", "2024-06-20", "M")
    val child =
        TestFixtures.mockChild(
            id = 2,
            name = "Baby Bob",
            date_of_birth = "2024-06-20",
            gender = "M",
            last_feeding = null,
            last_diaper_change = null,
            last_nap = null)
    io.mockk.coEvery { apiService.createChild(request) } returns child
    io.mockk.coEvery { childDao.upsertChild(any()) } returns Unit

    val result = repository.createChild(request)

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals("Baby Bob", result.data.name)
  }

  @Test
  fun `createChild http error returns Error`() = runTest {
    val request = CreateChildRequest("Baby", "2024-01-01", "F")
    val errorResponse = retrofit2.Response.error<Child>(400, "Bad Request".toResponseBody(null))
    io.mockk.coEvery { apiService.createChild(any()) } throws retrofit2.HttpException(errorResponse)

    val result = repository.createChild(request)

    assertIs<ApiResult.Error<Child>>(result)
    assertIs<ApiError.HttpError>(result.error)
  }

  @Test
  fun `updateChild success upserts and returns Success`() = runTest {
    val request = CreateChildRequest("Baby Alice Updated", "2024-01-15", "F")
    val child =
        TestFixtures.mockChild(name = "Baby Alice Updated", updated_at = "2024-01-15T12:00:00Z")
    io.mockk.coEvery { apiService.updateChild(1, request) } returns child
    io.mockk.coEvery { childDao.upsertChild(any()) } returns Unit

    val result = repository.updateChild(1, request)

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals("Baby Alice Updated", result.data.name)
  }

  @Test
  fun `deleteChild success removes from cache and returns Success`() = runTest {
    io.mockk.coEvery { apiService.deleteChild(1) } returns Unit
    io.mockk.coEvery { childDao.deleteChild(1) } returns Unit

    val result = repository.deleteChild(1)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteChild api error returns Error`() = runTest {
    io.mockk.coEvery { apiService.deleteChild(1) } throws IOException("Network down")

    val result = repository.deleteChild(1)

    assertIs<ApiResult.Error<Unit>>(result)
  }
}

package net.poopyfeed.pf.reports

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockRepo: AnalyticsRepository
  private lateinit var mockTracker: AnalyticsTracker
  private lateinit var appContext: Context

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepo = mockk(relaxed = true)
    mockTracker = mockk(relaxed = true)
    appContext = ApplicationProvider.getApplicationContext()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(childId: Int = 1): ReportsViewModel {
    val handle = SavedStateHandle(mapOf("childId" to childId))
    return ReportsViewModel(handle, mockRepo, mockTracker, appContext, testDispatcher)
  }

  @Test
  fun `exportCsv success emits CsvReady with uri`() = runTest {
    val mockBody = mockk<ResponseBody>(relaxed = true)
    every { mockBody.byteStream() } returns "csv-data".byteInputStream()
    coEvery { mockRepo.exportCsv(1, 30) } returns ApiResult.Success(mockBody)

    val vm = createViewModel()
    advanceUntilIdle()

    vm.exportCsv(30)
    advanceUntilIdle()

    val state = vm.exportState.first()
    assertTrue(state is ExportState.CsvReady)
    assertNotNull((state as ExportState.CsvReady).uri)
  }

  @Test
  fun `exportCsv success logs analytics event`() = runTest {
    val mockBody = mockk<ResponseBody>(relaxed = true)
    every { mockBody.byteStream() } returns "csv-data".byteInputStream()
    coEvery { mockRepo.exportCsv(1, 30) } returns ApiResult.Success(mockBody)

    val vm = createViewModel()
    advanceUntilIdle()

    vm.exportCsv(30)
    advanceUntilIdle()

    verify { mockTracker.logEvent("export_csv") }
  }

  @Test
  fun `exportCsv error emits ExportError`() = runTest {
    coEvery { mockRepo.exportCsv(1, 30) } returns ApiResult.Error(ApiError.NetworkError("fail"))

    val vm = createViewModel()
    advanceUntilIdle()

    vm.exportCsv(30)
    advanceUntilIdle()

    val state = vm.exportState.first()
    assertTrue(state is ExportState.Error)
    assertEquals("fail", (state as ExportState.Error).message)
  }

  @Test
  fun `startPdfExport success emits PdfStarted with taskId`() = runTest {
    val mockResponse = TestFixtures.mockExportJobResponse()
    coEvery { mockRepo.exportPdf(1, 30) } returns ApiResult.Success(mockResponse)

    val vm = createViewModel()
    advanceUntilIdle()

    vm.startPdfExport(30)
    advanceUntilIdle()

    val state = vm.exportState.first()
    assertTrue(state is ExportState.PdfStarted)
    assertEquals("task-123", (state as ExportState.PdfStarted).taskId)
  }

  @Test
  fun `startPdfExport success logs analytics event`() = runTest {
    val mockResponse = TestFixtures.mockExportJobResponse()
    coEvery { mockRepo.exportPdf(1, 30) } returns ApiResult.Success(mockResponse)

    val vm = createViewModel()
    advanceUntilIdle()

    vm.startPdfExport(30)
    advanceUntilIdle()

    verify { mockTracker.logEvent("export_pdf") }
  }

  @Test
  fun `startPdfExport error emits ExportError`() = runTest {
    coEvery { mockRepo.exportPdf(1, 30) } returns ApiResult.Error(ApiError.NetworkError("fail"))

    val vm = createViewModel()
    advanceUntilIdle()

    vm.startPdfExport(30)
    advanceUntilIdle()

    val state = vm.exportState.first()
    assertTrue(state is ExportState.Error)
  }

  @Test
  fun `clearExportState resets to Idle`() = runTest {
    val mockBody = mockk<ResponseBody>(relaxed = true)
    every { mockBody.byteStream() } returns "csv-data".byteInputStream()
    coEvery { mockRepo.exportCsv(1, 30) } returns ApiResult.Success(mockBody)

    val vm = createViewModel()
    advanceUntilIdle()

    vm.exportCsv(30)
    advanceUntilIdle()
    vm.clearExportState()

    val state = vm.exportState.first()
    assertTrue(state is ExportState.Idle)
  }
}

package net.poopyfeed.pf.reports

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
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
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.JobResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ExportPdfViewModelTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockRepo: AnalyticsRepository
  private lateinit var mockContext: Context

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepo = mockk(relaxed = true)
    mockContext = mockk(relaxed = true)
    every { mockContext.cacheDir } returns tempFolder.root
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(
      childId: Int = 1,
      taskId: String = "task-123",
  ): ExportPdfViewModel {
    val handle = SavedStateHandle(mapOf("childId" to childId, "taskId" to taskId))
    return ExportPdfViewModel(handle, mockRepo, mockContext, testDispatcher)
  }

  @Test
  fun `initial state is Polling`() = runTest {
    val vm = createViewModel()
    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Polling)
  }

  @Test
  fun `pollOnce completed emits Completed with download URL`() = runTest {
    val jobResult =
        JobResult(
            filename = "report.pdf",
            downloadUrl = "/analytics/download/report.pdf/",
        )
    val statusResponse =
        TestFixtures.mockJobStatusResponse(
            status = "completed",
            progress = 100,
            result = jobResult,
        )
    coEvery { mockRepo.getExportStatus(1, "task-123") } returns ApiResult.Success(statusResponse)

    val vm = createViewModel()
    vm.pollOnce()
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Completed)
    assertEquals("report.pdf", (state as PdfExportUiState.Completed).filename)
    assertEquals("/analytics/download/report.pdf/", state.downloadUrl)
  }

  @Test
  fun `pollOnce processing emits Polling with progress`() = runTest {
    val statusResponse =
        TestFixtures.mockJobStatusResponse(
            status = "processing",
            progress = 60,
        )
    coEvery { mockRepo.getExportStatus(1, "task-123") } returns ApiResult.Success(statusResponse)

    val vm = createViewModel()
    vm.pollOnce()
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Polling)
    assertEquals(60, (state as PdfExportUiState.Polling).progress)
    assertEquals("Generating report…", state.statusText)
  }

  @Test
  fun `pollOnce low progress shows Preparing status text`() = runTest {
    val statusResponse =
        TestFixtures.mockJobStatusResponse(
            status = "pending",
            progress = 5,
        )
    coEvery { mockRepo.getExportStatus(1, "task-123") } returns ApiResult.Success(statusResponse)

    val vm = createViewModel()
    vm.pollOnce()
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Polling)
    assertEquals("Preparing…", (state as PdfExportUiState.Polling).statusText)
  }

  @Test
  fun `pollOnce failed emits Failed`() = runTest {
    val statusResponse =
        TestFixtures.mockJobStatusResponse(
            status = "failed",
            error = "Server error",
        )
    coEvery { mockRepo.getExportStatus(1, "task-123") } returns ApiResult.Success(statusResponse)

    val vm = createViewModel()
    vm.pollOnce()
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Failed)
    assertEquals("Server error", (state as PdfExportUiState.Failed).message)
  }

  @Test
  fun `pollOnce network error emits Failed`() = runTest {
    coEvery { mockRepo.getExportStatus(1, "task-123") } returns
        ApiResult.Error(ApiError.NetworkError("No internet"))

    val vm = createViewModel()
    vm.pollOnce()
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Failed)
    assertEquals("No internet", (state as PdfExportUiState.Failed).message)
  }

  @Test
  fun `downloadFile success emits Downloaded with file`() = runTest {
    val mockBody = mockk<ResponseBody>(relaxed = true)
    every { mockBody.byteStream() } returns "pdf-data".byteInputStream()
    coEvery { mockRepo.downloadPdf("report.pdf") } returns ApiResult.Success(mockBody)

    val vm = createViewModel()
    vm.downloadFile("report.pdf")
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Downloaded)
    assertTrue((state as PdfExportUiState.Downloaded).file.exists())
  }

  @Test
  fun `downloadFile error emits Failed`() = runTest {
    coEvery { mockRepo.downloadPdf("report.pdf") } returns
        ApiResult.Error(ApiError.HttpError(500, "Internal Server Error"))

    val vm = createViewModel()
    vm.downloadFile("report.pdf")
    advanceUntilIdle()

    val state = vm.uiState.first()
    assertTrue(state is PdfExportUiState.Failed)
    assertEquals("Internal Server Error", (state as PdfExportUiState.Failed).message)
  }
}

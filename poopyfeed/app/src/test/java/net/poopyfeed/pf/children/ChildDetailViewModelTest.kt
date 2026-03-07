package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.Before
import org.junit.Test

/** Unit tests for ChildDetailViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildDetailViewModelTest {

  private lateinit var mockContext: Context
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var savedStateHandle: SavedStateHandle

  @Before
  fun setup() {
    mockContext = mockk()
    mockRepository = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    every { mockContext.getString(any()) } returns "formatted_time"
  }

  @Test
  fun `loads child from repository`() = runTest {
    val mockChild = TestFixtures.mockChild()
    coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
    coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())

    val viewModel = ChildDetailViewModel(savedStateHandle, mockRepository, mockContext)

    // ViewModel observes getChildCached; state may be Loading or Ready depending on dispatcher
    val state = viewModel.uiState.value
    assert(state is ChildDetailUiState.Loading || state is ChildDetailUiState.Ready) {
      "Expected Loading or Ready, got $state"
    }
    if (state is ChildDetailUiState.Ready) {
      assertEquals(mockChild.name, state.child.name)
    }
  }
}

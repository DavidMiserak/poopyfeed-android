package net.poopyfeed.pf

import android.view.View
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [ChildrenListFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ChildrenListFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: CachedChildrenRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
  }

  /**
   * Set NavController on container so it is available when fragment reaches STARTED (before add).
   */
  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.ChildrenListFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  /**
   * Launch fragment with mocked repo state. Idles the main looper so ViewModel and fragment
   * coroutines run.
   */
  private fun launchWithState(
      children: List<net.poopyfeed.pf.data.models.Child> = emptyList(),
      hasSynced: Boolean = true,
      refreshResult: ApiResult<List<net.poopyfeed.pf.data.models.Child>>? = null,
  ): ChildrenListFragment {
    every { repo.listChildrenCached() } returns flowOf(ApiResult.Success(children))
    every { repo.hasSyncedFlow } returns MutableStateFlow(hasSynced)
    coEvery { repo.refreshChildren() } returns (refreshResult ?: ApiResult.Success(children))

    var fragment: ChildrenListFragment? = null
    launchFragmentInHiltContainer<ChildrenListFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(20) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `shows ready state with children`() {
    val children =
        listOf(
            TestFixtures.mockChild(id = 1, name = "Alice"),
            TestFixtures.mockChild(id = 2, name = "Bob"),
        )
    val fragment = launchWithState(children = children)

    val root = fragment.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_children).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
  }

  @Test
  fun `shows empty state when no children`() {
    val fragment = launchWithState(children = emptyList(), hasSynced = true)

    val root = fragment.requireView()
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_children).visibility)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
  }

  @Test
  fun `shows error state on refresh failure`() {
    // Use replayed flow so observeChildren() gets Loading before refresh() runs; otherwise
    // refresh() can set Error then observeChildren overwrites with Loading.
    val listFlow =
        MutableSharedFlow<ApiResult<List<net.poopyfeed.pf.data.models.Child>>>(replay = 1)
    listFlow.tryEmit(ApiResult.Success(emptyList()))
    every { repo.listChildrenCached() } returns listFlow
    every { repo.hasSyncedFlow } returns MutableStateFlow(false)
    coEvery { repo.refreshChildren() } coAnswers
        {
          kotlinx.coroutines.delay(10)
          ApiResult.Error(ApiError.HttpError(500, "Server Error", "Something went wrong"))
        }

    var fragment: ChildrenListFragment? = null
    launchFragmentInHiltContainer<ChildrenListFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    // Idle enough for: observeChildren to set Loading, refresh() to resume after delay and set
    // Error, fragment collect to update UI
    repeat(80) { ShadowLooper.idleMainLooper() }

    coVerify { repo.refreshChildren() }
    val f = fragment!!
    val root = f.requireView()
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_children).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(
        "error state should be VISIBLE",
        View.VISIBLE,
        root.findViewById<View>(R.id.layout_error_state).visibility,
    )

    val errorText = root.findViewById<TextView>(R.id.text_error_message)
    assertTrue("Error message should not be empty", errorText.text.isNotEmpty())
  }

  @Test
  fun `retry button triggers refresh`() {
    val fragment =
        launchWithState(
            hasSynced = false,
            refreshResult = ApiResult.Error(ApiError.NetworkError("fail")),
        )

    coEvery { repo.refreshChildren() } returns ApiResult.Success(emptyList())

    val retryButton = fragment.requireView().findViewById<View>(R.id.button_retry)
    retryButton.performClick()
    repeat(5) { ShadowLooper.idleMainLooper() }

    coVerify(atLeast = 2) { repo.refreshChildren() }
  }

  @Test
  fun `swipe refresh stops after refresh completes`() {
    val children = listOf(TestFixtures.mockChild())
    val fragment = launchWithState(children = children)

    val swipeRefresh =
        fragment.requireView() as androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    assertEquals(false, swipeRefresh.isRefreshing)
  }
}

package net.poopyfeed.pf.diapers

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [DiapersListFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DiapersListFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: CachedDiapersRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  private val childId = 1

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    every { repo.listDiapersCached(childId) } returns flowOf(ApiResult.Success(emptyList()))
    every { repo.hasSyncedFlow(childId) } returns flowOf(true)
    coEvery { repo.refreshDiapers(childId) } returns ApiResult.Success(emptyList())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(
        R.id.DiapersListFragment,
        android.os.Bundle().apply { putInt("childId", childId) },
    )
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): DiapersListFragment {
    var fragment: DiapersListFragment? = null
    launchFragmentInHiltContainer<DiapersListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `ready state with diapers shows recycler`() {
    val diapers = listOf(TestFixtures.mockDiaper(id = 1))
    every { repo.listDiapersCached(childId) } returns flowOf(ApiResult.Success(diapers))
    coEvery { repo.refreshDiapers(childId) } returns ApiResult.Success(diapers)

    var fragment: DiapersListFragment? = null
    launchFragmentInHiltContainer<DiapersListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_diapers).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `empty state shows empty layout and hides recycler`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_diapers).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `error state shows error layout when cache returns error`() {
    every { repo.listDiapersCached(childId) } returns
        flowOf(
            ApiResult.Error(
                net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server error")))
    every { repo.hasSyncedFlow(childId) } returns flowOf(true)
    coEvery { repo.refreshDiapers(childId) } returns ApiResult.Success(emptyList())

    var fragment: DiapersListFragment? = null
    launchFragmentInHiltContainer<DiapersListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(
        "Error state should be visible",
        View.VISIBLE,
        root.findViewById<View>(R.id.layout_error_state).visibility,
    )
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_diapers).visibility)
  }
}

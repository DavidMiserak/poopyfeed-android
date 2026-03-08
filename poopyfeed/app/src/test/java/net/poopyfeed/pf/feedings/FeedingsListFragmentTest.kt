package net.poopyfeed.pf.feedings

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
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [FeedingsListFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FeedingsListFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: CachedFeedingsRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  private val childId = 1

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    every { repo.listFeedingsCached(childId) } returns flowOf(ApiResult.Success(emptyList()))
    every { repo.hasSyncedFlow(childId) } returns flowOf(true)
    coEvery { repo.refreshFeedings(childId) } returns ApiResult.Success(emptyList())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(
        R.id.FeedingsListFragment,
        android.os.Bundle().apply { putInt("childId", childId) },
    )
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): FeedingsListFragment {
    var fragment: FeedingsListFragment? = null
    launchFragmentInHiltContainer<FeedingsListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.let { v ->
        v.findViewById<View>(R.id.recycler_feedings).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_empty_state).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_error_state).visibility == View.VISIBLE
      } == true
    }
    return fragment!!
  }

  @Test
  fun `ready state with feedings shows recycler`() {
    val feedings = listOf(TestFixtures.mockFeeding(id = 1))
    every { repo.listFeedingsCached(childId) } returns flowOf(ApiResult.Success(feedings))
    coEvery { repo.refreshFeedings(childId) } returns ApiResult.Success(feedings)

    var fragment: FeedingsListFragment? = null
    launchFragmentInHiltContainer<FeedingsListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.findViewById<View>(R.id.recycler_feedings)?.visibility == View.VISIBLE
    }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_feedings).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `empty state shows empty layout and hides recycler`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_feedings).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `error state shows error layout when cache returns error`() {
    every { repo.listFeedingsCached(childId) } returns
        flowOf(
            ApiResult.Error(
                net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server error")))
    every { repo.hasSyncedFlow(childId) } returns flowOf(true)
    coEvery { repo.refreshFeedings(childId) } returns ApiResult.Success(emptyList())

    var fragment: FeedingsListFragment? = null
    launchFragmentInHiltContainer<FeedingsListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.findViewById<View>(R.id.layout_error_state)?.visibility == View.VISIBLE
    }

    val root = fragment!!.requireView()
    assertEquals(
        "Error state should be visible",
        View.VISIBLE,
        root.findViewById<View>(R.id.layout_error_state).visibility,
    )
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_feedings).visibility)
  }
}

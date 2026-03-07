package net.poopyfeed.pf.notifications

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [NotificationsFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class NotificationsFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: NotificationsRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    val list =
        PaginatedResponse(
            count = 1,
            next = null,
            previous = null,
            results = listOf(TestFixtures.mockNotification()),
        )
    coEvery { repo.listNotifications(page = 1) } returns ApiResult.Success(list)
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.NotificationsFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): NotificationsFragment {
    var fragment: NotificationsFragment? = null
    launchFragmentInHiltContainer<NotificationsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `ready state shows recycler and hides loading`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_notifications).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `empty state shows empty layout`() {
    coEvery { repo.listNotifications(page = 1) } returns
        ApiResult.Success(
            PaginatedResponse(count = 0, next = null, previous = null, results = emptyList()))

    var fragment: NotificationsFragment? = null
    launchFragmentInHiltContainer<NotificationsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_notifications).visibility)
  }

  @Test
  fun `error state shows error layout`() {
    coEvery { repo.listNotifications(page = 1) } returns
        ApiResult.Error(
            net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server error"))

    var fragment: NotificationsFragment? = null
    launchFragmentInHiltContainer<NotificationsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_error_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_notifications).visibility)
  }
}

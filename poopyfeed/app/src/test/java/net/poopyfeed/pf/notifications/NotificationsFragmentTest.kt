package net.poopyfeed.pf.notifications

import android.os.Build
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
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

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
    idleMainLooperUntil {
      fragment?.view?.let { v ->
        v.findViewById<View>(R.id.recycler_notifications).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_empty_state).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_error_state).visibility == View.VISIBLE
      } == true
    }
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
    idleMainLooperUntil {
      fragment?.view?.findViewById<View>(R.id.layout_empty_state)?.visibility == View.VISIBLE
    }

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
    idleMainLooperUntil {
      fragment?.view?.findViewById<View>(R.id.layout_error_state)?.visibility == View.VISIBLE
    }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_error_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_notifications).visibility)
  }

  @Test
  fun `permission request on API 33+ does not crash fragment`() {
    // Set SDK to 33 (API 33+) so permission request logic runs
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 33)

    // Robolectric denies all permissions by default
    var fragment: NotificationsFragment? = null
    launchFragmentInHiltContainer<NotificationsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.let { v ->
        v.findViewById<View>(R.id.recycler_notifications).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_empty_state).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_error_state).visibility == View.VISIBLE
      } == true
    }

    val root = fragment!!.requireView()
    // Verify fragment loads gracefully despite permission request
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_notifications).visibility)
  }

  @Test
  fun `permission logic skipped on API below 33`() {
    // Set SDK to 32 (below API 33) so permission request logic doesn't run
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 32)

    var fragment: NotificationsFragment? = null
    launchFragmentInHiltContainer<NotificationsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.let { v ->
        v.findViewById<View>(R.id.recycler_notifications).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_empty_state).visibility == View.VISIBLE ||
            v.findViewById<View>(R.id.layout_error_state).visibility == View.VISIBLE
      } == true
    }

    val root = fragment!!.requireView()
    // Verify fragment loads normally on pre-API 33
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_notifications).visibility)
  }
}

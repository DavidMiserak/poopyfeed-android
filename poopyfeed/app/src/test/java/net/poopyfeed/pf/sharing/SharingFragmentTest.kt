package net.poopyfeed.pf.sharing

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
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.SharingRepository
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [SharingFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SharingFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val sharingRepository: SharingRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  private val childId = 7

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    coEvery { sharingRepository.listInvites(childId) } returns ApiResult.Success(emptyList())
    coEvery { sharingRepository.listShares(childId) } returns
        ApiResult.Success(listOf(TestFixtures.mockChildShare(id = 1)))
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(
        R.id.sharingFragment,
        android.os.Bundle().apply { putInt("childId", childId) },
    )
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): SharingFragment {
    var fragment: SharingFragment? = null
    launchFragmentInHiltContainer<SharingFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `ready state shows recycler and FAB visible`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_sharing).visibility)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.fab_invite).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `error state shows error layout and hides list`() {
    coEvery { sharingRepository.listInvites(childId) } returns
        ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server error"))

    var fragment: SharingFragment? = null
    launchFragmentInHiltContainer<SharingFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_error_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_sharing).visibility)
  }
}

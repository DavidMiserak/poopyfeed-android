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
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.ChildrenRepository
import net.poopyfeed.pf.data.repository.SharingRepository
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [PendingInvitesFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PendingInvitesFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val sharingRepository: SharingRepository = mockk(relaxed = true)

  @BindValue @JvmField val childrenRepository: ChildrenRepository = mockk(relaxed = true)

  @BindValue @JvmField val cachedChildrenRepository: CachedChildrenRepository =
      mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    coEvery { sharingRepository.getPendingInvites() } returns ApiResult.Success(emptyList())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.pendingInvitesFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): PendingInvitesFragment {
    var fragment: PendingInvitesFragment? = null
    launchFragmentInHiltContainer<PendingInvitesFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `empty state shows empty layout and hides recycler`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_empty_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_pending_invites).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_loading).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_error_state).visibility)
  }

  @Test
  fun `ready state with invites shows recycler`() {
    val invite = TestFixtures.mockShareInvite(id = 1, child = 5)
    coEvery { sharingRepository.getPendingInvites() } returns
        ApiResult.Success(listOf(invite))
    every { childrenRepository.getChild(5) } returns
        flowOf(ApiResult.Success(TestFixtures.mockChild(id = 5, name = "Baby Sam")))

    var fragment: PendingInvitesFragment? = null
    launchFragmentInHiltContainer<PendingInvitesFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_pending_invites).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.layout_empty_state).visibility)
  }

  @Test
  fun `error state shows error layout`() {
    coEvery { sharingRepository.getPendingInvites() } returns
        ApiResult.Error(
            net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server error"))

    var fragment: PendingInvitesFragment? = null
    launchFragmentInHiltContainer<PendingInvitesFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    val root = fragment!!.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layout_error_state).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_pending_invites).visibility)
  }
}

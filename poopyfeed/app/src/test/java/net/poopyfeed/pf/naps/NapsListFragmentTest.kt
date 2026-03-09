package net.poopyfeed.pf.naps

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [NapsListFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class NapsListFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: CachedNapsRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  private val childId = 1

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    every { repo.pagedNaps(childId) } returns
        flowOf(PagingData.empty<Nap>())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(
        R.id.NapsListFragment,
        android.os.Bundle().apply { putInt("childId", childId) },
    )
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): NapsListFragment {
    var fragment: NapsListFragment? = null
    launchFragmentInHiltContainer<NapsListFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil { fragment?.view != null }
    return fragment!!
  }

  @Test
  fun `fragment launches without errors`() {
    val fragment = launchFragment()
    val root = fragment.requireView()
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.recycler_naps).visibility)
  }

  @Test
  fun `adapter is set up on recycler`() {
    val fragment = launchFragment()
    val root = fragment.requireView()
    val recycler = root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_naps)
    assert(recycler.adapter != null)
  }
}

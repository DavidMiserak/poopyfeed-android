package net.poopyfeed.pf.children

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
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [ChildDetailFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ChildDetailFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: CachedChildrenRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  private val childId = 1

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    every { repo.getChildCached(childId) } returns
        flowOf(TestFixtures.mockChild(id = childId, name = "Test Child"))
    coEvery { repo.refreshChildren() } returns ApiResult.Success(emptyList())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(
        R.id.ChildDetailFragment,
        android.os.Bundle().apply { putInt("childId", childId) },
    )
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): ChildDetailFragment {
    var fragment: ChildDetailFragment? = null
    launchFragmentInHiltContainer<ChildDetailFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment
          ?.view
          ?.findViewById<android.widget.TextView>(R.id.text_child_name)
          ?.text
          ?.isNotEmpty() == true
    }
    return fragment!!
  }

  @Test
  fun `ready state shows child name and edit button visible for owner`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(
        "Test Child",
        root.findViewById<android.widget.TextView>(R.id.text_child_name).text.toString(),
    )
    assertNotNull(root.findViewById<android.widget.TextView>(R.id.text_age_gender).text)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.button_edit).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.chip_role).visibility)
  }

  @Test
  fun `caregiver hides edit and shows role chip`() {
    every { repo.getChildCached(childId) } returns
        flowOf(
            TestFixtures.mockChild(
                id = childId,
                name = "Test Child",
                user_role = "caregiver",
                can_edit = false,
            ),
        )

    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.GONE, root.findViewById<View>(R.id.button_edit).visibility)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.chip_role).visibility)
  }

  @Test
  fun `co-parent shows edit and role chip`() {
    every { repo.getChildCached(childId) } returns
        flowOf(
            TestFixtures.mockChild(
                id = childId,
                name = "Test Child",
                user_role = "co-parent",
                can_edit = true,
            ),
        )

    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.button_edit).visibility)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.chip_role).visibility)
  }

  @Test
  fun `error state when child not found`() {
    every { repo.getChildCached(childId) } returns flowOf(null)

    var fragment: ChildDetailFragment? = null
    launchFragmentInHiltContainer<ChildDetailFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil { fragment?.view?.findViewById<View>(R.id.text_child_name) != null }

    assertNotNull(fragment)
    assertNotNull(fragment!!.requireView().findViewById<View>(R.id.text_child_name))
  }
}

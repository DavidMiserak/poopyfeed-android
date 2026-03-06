package net.poopyfeed.pf

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
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

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
    repeat(40) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `ready state shows child name and owner buttons visible`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(
        "Test Child",
        root.findViewById<android.widget.TextView>(R.id.text_child_name).text.toString(),
    )
    assertNotNull(root.findViewById<android.widget.TextView>(R.id.text_age_gender).text)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.button_delete).visibility)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.button_edit).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.chip_role).visibility)
  }

  @Test
  fun `non-owner hides delete and edit and shows role chip`() {
    every { repo.getChildCached(childId) } returns
        flowOf(
            TestFixtures.mockChild(id = childId, name = "Test Child", user_role = "co-parent"),
        )

    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.GONE, root.findViewById<View>(R.id.button_delete).visibility)
    assertEquals(View.GONE, root.findViewById<View>(R.id.button_edit).visibility)
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.chip_role).visibility)
  }

  @Test
  fun `delete button opens confirmation dialog`() {
    val fragment = launchFragment()
    fragment.requireView().findViewById<View>(R.id.button_delete).performClick()
    repeat(5) { ShadowLooper.idleMainLooper() }

    assertNotNull(ShadowDialog.getLatestDialog())
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
    repeat(40) { ShadowLooper.idleMainLooper() }

    assertNotNull(fragment)
    assertNotNull(fragment!!.requireView().findViewById<View>(R.id.text_child_name))
  }
}

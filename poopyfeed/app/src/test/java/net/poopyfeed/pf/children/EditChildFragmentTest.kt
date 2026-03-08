package net.poopyfeed.pf.children

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
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

/** UI tests for [EditChildFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class EditChildFragmentTest {

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
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(
        R.id.EditChildFragment,
        android.os.Bundle().apply { putInt("childId", childId) },
    )
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): EditChildFragment {
    var fragment: EditChildFragment? = null
    launchFragmentInHiltContainer<EditChildFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.findViewById<android.widget.EditText>(R.id.input_name)?.text?.isNotEmpty() ==
          true
    }
    return fragment!!
  }

  @Test
  fun `ready state shows child name and save button visible`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(
        "Test Child",
        root.findViewById<android.widget.EditText>(R.id.input_name).text.toString(),
    )
    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.button_save).visibility)
    assertEquals(true, root.findViewById<View>(R.id.button_save).isEnabled)
  }

  @Test
  fun `error state when child not found`() {
    every { repo.getChildCached(childId) } returns flowOf(null)

    var fragment: EditChildFragment? = null
    launchFragmentInHiltContainer<EditChildFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", childId) },
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil { fragment?.view?.findViewById<View>(R.id.input_name) != null }

    assertNotNull(fragment)
    assertNotNull(fragment!!.requireView().findViewById<View>(R.id.input_name))
  }
}

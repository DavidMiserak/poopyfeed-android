package net.poopyfeed.pf.children

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateChildRequest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [CreateChildBottomSheetFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CreateChildBottomSheetFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val repo: CachedChildrenRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.ChildrenListFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): CreateChildBottomSheetFragment {
    var fragment: CreateChildBottomSheetFragment? = null
    launchFragmentInHiltContainer<CreateChildBottomSheetFragment>(
        beforeAdd = ::installNavController,
    ) {
      fragment = this
    }
    idleMainLooperUntil { fragment?.view?.findViewById<View>(R.id.button_save)?.isEnabled == true }
    return fragment!!
  }

  private fun setSelectedDate(fragment: CreateChildBottomSheetFragment, date: String) {
    val field = CreateChildBottomSheetFragment::class.java.getDeclaredField("selectedDate")
    field.isAccessible = true
    field.set(fragment, date)
  }

  @Test
  fun `idle state shows save button enabled and progress hidden`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.VISIBLE, root.findViewById<View>(R.id.button_save).visibility)
    assertEquals(true, root.findViewById<View>(R.id.button_save).isEnabled)
    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_saving).visibility)
  }

  @Test
  fun `empty name shows validation error`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    setSelectedDate(fragment, "2024-01-15")
    root.findViewById<android.widget.EditText>(R.id.input_dob).setText("2024-01-15")
    root.findViewById<View>(R.id.radio_girl).performClick()
    root.findViewById<View>(R.id.button_save).performClick()
    idleMainLooperUntil { root.findViewById<TextInputLayout>(R.id.layout_name).error != null }

    val nameLayout = root.findViewById<TextInputLayout>(R.id.layout_name)
    assertNotNull(nameLayout.error)
    assertTrue(
        nameLayout.error?.toString()?.contains("required", ignoreCase = true) == true ||
            nameLayout.error?.toString() ==
                getInstrumentation().targetContext.getString(R.string.create_child_name_error),
    )
    coVerify(exactly = 0) { repo.createChild(CreateChildRequest("", "2024-01-15", "F")) }
  }

  @Test
  fun `valid form calls createChild and on success dismisses`() {
    coEvery { repo.createChild(CreateChildRequest("Baby", "2024-01-15", "F")) } returns
        ApiResult.Success(TestFixtures.mockChild(id = 99, name = "Baby"))

    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.input_name).setText("Baby")
    setSelectedDate(fragment, "2024-01-15")
    root.findViewById<android.widget.EditText>(R.id.input_dob).setText("2024-01-15")
    root.findViewById<View>(R.id.radio_girl).performClick()
    root.findViewById<View>(R.id.button_save).performClick()
    idleMainLooperUntil { !fragment.isAdded }

    coVerify { repo.createChild(CreateChildRequest("Baby", "2024-01-15", "F")) }
  }
}

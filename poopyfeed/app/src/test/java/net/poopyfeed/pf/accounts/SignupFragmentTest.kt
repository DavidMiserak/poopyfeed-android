package net.poopyfeed.pf.accounts

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
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [SignupFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SignupFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val authRepository: AuthRepository = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.navigate(R.id.SignupFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): SignupFragment {
    var fragment: SignupFragment? = null
    launchFragmentInHiltContainer<SignupFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    idleMainLooperUntil {
      fragment?.view?.findViewById<View>(R.id.button_signup)?.isEnabled == true
    }
    return fragment!!
  }

  @Test
  fun `idle state shows form with signup button enabled and progress hidden`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_signup).visibility)
    assertNotNull(root.findViewById<View>(R.id.button_signup))
    assertEquals(true, root.findViewById<View>(R.id.button_signup).isEnabled)
  }

  @Test
  fun `invalid email shows error and does not call signup`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_signup_email).setText("not-an-email")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_password)
        .setText("password123")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_confirm_password)
        .setText("password123")
    root.findViewById<View>(R.id.button_signup).performClick()
    idleMainLooperUntil {
      root.findViewById<TextInputLayout>(R.id.input_layout_signup_email).error != null
    }

    val emailLayout = root.findViewById<TextInputLayout>(R.id.input_layout_signup_email)
    assertNotNull(emailLayout.error)
    assertEquals(
        getInstrumentation().targetContext.getString(R.string.signup_email_error),
        emailLayout.error?.toString(),
    )
    coVerify(exactly = 0) { authRepository.signup("not-an-email", "password123") }
  }

  @Test
  fun `short password shows error and does not call signup`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_signup_email).setText("a@b.co")
    root.findViewById<android.widget.EditText>(R.id.edit_text_signup_password).setText("short")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_confirm_password)
        .setText("short")
    root.findViewById<View>(R.id.button_signup).performClick()
    idleMainLooperUntil {
      root.findViewById<TextInputLayout>(R.id.input_layout_signup_password).error != null
    }

    val passwordLayout = root.findViewById<TextInputLayout>(R.id.input_layout_signup_password)
    assertNotNull(passwordLayout.error)
    assertEquals(
        getInstrumentation().targetContext.getString(R.string.signup_password_error),
        passwordLayout.error?.toString(),
    )
    coVerify(exactly = 0) { authRepository.signup("a@b.co", "short") }
  }

  @Test
  fun `confirm password mismatch shows error and does not call signup`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_signup_email).setText("a@b.co")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_password)
        .setText("password123")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_confirm_password)
        .setText("different")
    root.findViewById<View>(R.id.button_signup).performClick()
    idleMainLooperUntil {
      root.findViewById<TextInputLayout>(R.id.input_layout_signup_confirm_password).error != null
    }

    val confirmLayout =
        root.findViewById<TextInputLayout>(R.id.input_layout_signup_confirm_password)
    assertNotNull(confirmLayout.error)
    assertEquals(
        getInstrumentation().targetContext.getString(R.string.signup_confirm_password_error),
        confirmLayout.error?.toString(),
    )
    coVerify(exactly = 0) { authRepository.signup("a@b.co", "password123") }
  }

  @Test
  fun `go to login navigates back to LoginFragment`() {
    val fragment = launchFragment()
    val loginLink = fragment.requireView().findViewById<View>(R.id.text_go_to_login)
    loginLink.performClick()
    idleMainLooperUntil { navController.currentDestination?.id == R.id.LoginFragment }

    assertEquals(R.id.LoginFragment, navController.currentDestination?.id)
  }

  @Test
  fun `valid signup success navigates to ChildrenListFragment`() {
    coEvery { authRepository.signup("a@b.co", "password123") } returns
        ApiResult.Success("token-123")
    coEvery { authRepository.getProfile() } returns
        ApiResult.Success(TestFixtures.mockUserProfile())

    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_signup_email).setText("a@b.co")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_password)
        .setText("password123")
    root
        .findViewById<android.widget.EditText>(R.id.edit_text_signup_confirm_password)
        .setText("password123")
    root.findViewById<View>(R.id.button_signup).performClick()
    idleMainLooperUntil { navController.currentDestination?.id == R.id.ChildrenListFragment }

    assertEquals(R.id.ChildrenListFragment, navController.currentDestination?.id)
  }
}

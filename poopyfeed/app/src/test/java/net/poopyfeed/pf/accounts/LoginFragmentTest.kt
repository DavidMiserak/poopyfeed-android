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
import io.mockk.every
import io.mockk.mockk
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [LoginFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LoginFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val authRepository: AuthRepository = mockk(relaxed = true)

  @BindValue @JvmField val tokenManager: TokenManager = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    every { tokenManager.getToken() } returns null
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.LoginFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): LoginFragment {
    var fragment: LoginFragment? = null
    launchFragmentInHiltContainer<LoginFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    idleMainLooperUntil { fragment?.view?.findViewById<View>(R.id.button_login)?.isEnabled == true }
    return fragment!!
  }

  @Test
  fun `idle state shows form with login button enabled and progress hidden`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(View.GONE, root.findViewById<View>(R.id.progress_login).visibility)
    assertNotNull(root.findViewById<View>(R.id.button_login))
    assertEquals(true, root.findViewById<View>(R.id.button_login).isEnabled)
  }

  @Test
  fun `invalid email shows error and does not call login`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_email).setText("not-an-email")
    root.findViewById<android.widget.EditText>(R.id.edit_text_password).setText("password123")
    root.findViewById<View>(R.id.button_login).performClick()
    idleMainLooperUntil {
      root.findViewById<TextInputLayout>(R.id.input_layout_email).error != null
    }

    val emailLayout = root.findViewById<TextInputLayout>(R.id.input_layout_email)
    assertNotNull(emailLayout.error)
    assertEquals(
        getInstrumentation().targetContext.getString(R.string.login_email_error),
        emailLayout.error?.toString(),
    )
    coVerify(exactly = 0) { authRepository.login("not-an-email", "password123") }
  }

  @Test
  fun `empty password shows error and does not call login`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_email).setText("user@example.com")
    root.findViewById<android.widget.EditText>(R.id.edit_text_password).setText("")
    root.findViewById<View>(R.id.button_login).performClick()
    idleMainLooperUntil {
      root.findViewById<TextInputLayout>(R.id.input_layout_password).error != null
    }

    val passwordLayout = root.findViewById<TextInputLayout>(R.id.input_layout_password)
    assertNotNull(passwordLayout.error)
    assertEquals(
        getInstrumentation().targetContext.getString(R.string.login_password_error),
        passwordLayout.error?.toString(),
    )
    coVerify(exactly = 0) { authRepository.login("user@example.com", "") }
  }

  @Test
  fun `go to signup navigates to SignupFragment`() {
    val fragment = launchFragment()
    val signupLink = fragment.requireView().findViewById<View>(R.id.text_go_to_signup)
    signupLink.performClick()
    idleMainLooperUntil { navController.currentDestination?.id == R.id.SignupFragment }

    assertEquals(R.id.SignupFragment, navController.currentDestination?.id)
  }

  @Test
  fun `valid credentials and login success navigates to ChildrenListFragment`() {
    coEvery { authRepository.login("user@example.com", "password123") } returns
        ApiResult.Success("token-123")
    coEvery { authRepository.getProfile() } returns
        ApiResult.Success(TestFixtures.mockUserProfile())

    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_email).setText("user@example.com")
    root.findViewById<android.widget.EditText>(R.id.edit_text_password).setText("password123")
    root.findViewById<View>(R.id.button_login).performClick()
    idleMainLooperUntil { navController.currentDestination?.id == R.id.ChildrenListFragment }

    assertEquals(R.id.ChildrenListFragment, navController.currentDestination?.id)
  }

  @Test
  fun `login error keeps fragment on login screen`() {
    coEvery { authRepository.login("user@example.com", "wrong") } returns
        ApiResult.Error(ApiError.HttpError(401, "Unauthorized", "Invalid credentials"))

    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_email).setText("user@example.com")
    root.findViewById<android.widget.EditText>(R.id.edit_text_password).setText("wrong")
    root.findViewById<View>(R.id.button_login).performClick()
    idleMainLooperUntil { root.findViewById<View>(R.id.progress_login).visibility == View.GONE }

    assertEquals(R.id.LoginFragment, navController.currentDestination?.id)
    assertEquals(true, root.findViewById<View>(R.id.button_login).isEnabled)
  }
}

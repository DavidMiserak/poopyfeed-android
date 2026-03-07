package net.poopyfeed.pf

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for the login flow.
 *
 * Requires a running backend at http://10.0.2.2:8000 (emulator) or updated API_BASE_URL for
 * physical device.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginE2eTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun loginScreen_isDisplayedOnLaunch() {
    onView(withId(R.id.edit_text_email)).check(matches(isDisplayed()))
    onView(withId(R.id.edit_text_password)).check(matches(isDisplayed()))
    onView(withId(R.id.button_login)).check(matches(isDisplayed()))
    onView(withId(R.id.button_login)).check(matches(isEnabled()))
  }

  @Test
  fun loginScreen_showsSignupLink() {
    onView(withId(R.id.text_go_to_signup)).check(matches(isDisplayed()))
  }

  @Test
  fun login_emptyEmail_showsValidationError() {
    onView(withId(R.id.edit_text_password)).perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.button_login)).perform(click())

    // Email field should show error via TextInputLayout
    onView(withId(R.id.input_layout_email)).check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun login_emptyPassword_showsValidationError() {
    onView(withId(R.id.edit_text_email))
        .perform(replaceText("test@example.com"), closeSoftKeyboard())
    onView(withId(R.id.button_login)).perform(click())

    onView(withId(R.id.input_layout_password)).check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun login_invalidEmail_showsValidationError() {
    onView(withId(R.id.edit_text_email)).perform(replaceText("not-an-email"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_password)).perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.button_login)).perform(click())

    onView(withId(R.id.input_layout_email)).check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun signupLink_navigatesToSignupScreen() {
    onView(withId(R.id.text_go_to_signup)).perform(click())

    // Signup screen should be visible (has its own email field)
    onView(withId(R.id.edit_text_signup_email)).check(matches(isDisplayed()))
  }
}

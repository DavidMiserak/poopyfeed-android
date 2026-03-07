package net.poopyfeed.pf

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
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
 * E2E tests for the signup flow. Navigates from login to signup first, then validates the signup
 * form behavior.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SignupE2eTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @Before
  fun setUp() {
    hiltRule.inject()
    // Navigate from login to signup
    onView(withId(R.id.text_go_to_signup)).perform(click())
  }

  @Test
  fun signupScreen_allFieldsDisplayed() {
    onView(withId(R.id.edit_text_signup_email)).check(matches(isDisplayed()))
    onView(withId(R.id.edit_text_signup_password)).check(matches(isDisplayed()))
    onView(withId(R.id.edit_text_signup_confirm_password)).check(matches(isDisplayed()))
    onView(withId(R.id.button_signup)).check(matches(isDisplayed()))
    onView(withId(R.id.button_signup)).check(matches(isEnabled()))
  }

  @Test
  fun signupScreen_showsLoginLink() {
    onView(withId(R.id.text_go_to_login)).check(matches(isDisplayed()))
  }

  @Test
  fun signup_emptyEmail_showsValidationError() {
    onView(withId(R.id.edit_text_signup_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_confirm_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.button_signup)).perform(click())

    onView(withId(R.id.input_layout_signup_email)).check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun signup_invalidEmail_showsValidationError() {
    onView(withId(R.id.edit_text_signup_email))
        .perform(replaceText("not-an-email"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_confirm_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.button_signup)).perform(click())

    onView(withId(R.id.input_layout_signup_email)).check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun signup_shortPassword_showsValidationError() {
    onView(withId(R.id.edit_text_signup_email))
        .perform(replaceText("test@example.com"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_password))
        .perform(replaceText("short"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_confirm_password))
        .perform(replaceText("short"), closeSoftKeyboard())
    onView(withId(R.id.button_signup)).perform(click())

    onView(withId(R.id.input_layout_signup_password)).check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun signup_mismatchedPasswords_showsValidationError() {
    onView(withId(R.id.edit_text_signup_email))
        .perform(replaceText("test@example.com"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_confirm_password))
        .perform(replaceText("different456"), closeSoftKeyboard())
    onView(withId(R.id.button_signup)).perform(click())

    onView(withId(R.id.input_layout_signup_confirm_password))
        .check(matches(hasTextInputLayoutError()))
  }

  @Test
  fun signup_validInputs_noValidationErrors() {
    onView(withId(R.id.edit_text_signup_email))
        .perform(replaceText("test@example.com"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.edit_text_signup_confirm_password))
        .perform(replaceText("password123"), closeSoftKeyboard())
    onView(withId(R.id.button_signup)).perform(click())

    // No validation errors should appear (API call happens but may fail without backend)
    onView(withId(R.id.input_layout_signup_email)).check(matches(not(hasTextInputLayoutError())))
    onView(withId(R.id.input_layout_signup_password)).check(matches(not(hasTextInputLayoutError())))
    onView(withId(R.id.input_layout_signup_confirm_password))
        .check(matches(not(hasTextInputLayoutError())))
  }

  @Test
  fun loginLink_navigatesBackToLogin() {
    onView(withId(R.id.text_go_to_login)).perform(click())

    // Should be back on login screen
    onView(withId(R.id.edit_text_email)).check(matches(isDisplayed()))
  }
}

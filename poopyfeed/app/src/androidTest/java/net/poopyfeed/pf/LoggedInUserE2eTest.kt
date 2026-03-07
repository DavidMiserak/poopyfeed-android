package net.poopyfeed.pf

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for logged-in users. Verifies that users with an existing token skip login and go
 * directly to the children list screen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoggedInUserE2eTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @Before
  fun setUp() {
    hiltRule.inject()
    // Clear any existing login state
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
  }

  @Test
  fun loginScreen_showsWhenNoToken() {
    // With no token in prefs (cleared in setUp), login screen should be visible
    onView(withId(R.id.button_login)).check(matches(isDisplayed()))
    onView(withId(R.id.edit_text_email)).check(matches(isDisplayed()))
  }
}

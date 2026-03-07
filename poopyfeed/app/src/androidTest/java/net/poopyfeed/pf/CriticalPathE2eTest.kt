package net.poopyfeed.pf

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for critical user paths: login → children list → child detail → tracking lists.
 *
 * **Requires a running backend** (e.g. emulator: http://10.0.2.2:8000). Set E2E_TEST_EMAIL and
 * E2E_TEST_PASSWORD (gradle -PE2E_TEST_EMAIL=... -PE2E_TEST_PASSWORD=...) for a test user that has
 * at least one child. If credentials are not set, tests are skipped.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CriticalPathE2eTest {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val activityRule = ActivityScenarioRule(MainActivity::class.java)

  private val email: String
    get() = BuildConfig.E2E_TEST_EMAIL

  private val password: String
    get() = BuildConfig.E2E_TEST_PASSWORD

  private fun requireCredentials() {
    assumeTrue(
        "E2E credentials not set. Use -PE2E_TEST_EMAIL=... -PE2E_TEST_PASSWORD=... for critical path E2E.",
        email.isNotEmpty() && password.isNotEmpty(),
    )
  }

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  /**
   * Perform login and wait for children list (successful login navigates to ChildrenListFragment).
   */
  private fun doLogin() {
    requireCredentials()
    onView(withId(R.id.edit_text_email)).perform(replaceText(email), closeSoftKeyboard())
    onView(withId(R.id.edit_text_password)).perform(replaceText(password), closeSoftKeyboard())
    onView(withId(R.id.button_login)).perform(click())
    // Wait for post-login screen: either list or empty state
    onView(withId(R.id.recycler_children)).check(matches(isDisplayed()))
  }

  /** From children list, open first child (assumes at least one child exists for test user). */
  private fun openFirstChild() {
    onView(withId(R.id.recycler_children))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    onView(withId(R.id.text_child_name)).check(matches(isDisplayed()))
  }

  @Test
  fun loginWithValidCredentials_navigatesToChildrenList() {
    requireCredentials()
    onView(withId(R.id.edit_text_email)).perform(replaceText(email), closeSoftKeyboard())
    onView(withId(R.id.edit_text_password)).perform(replaceText(password), closeSoftKeyboard())
    onView(withId(R.id.button_login)).perform(click())

    // Success: navigates to ChildrenListFragment (recycler or empty state visible)
    onView(withId(R.id.recycler_children)).check(matches(isDisplayed()))
  }

  @Test
  fun criticalPath_childDetailThenFeedingsList() {
    doLogin()
    openFirstChild()

    onView(withId(R.id.button_feedings)).perform(click())

    onView(withId(R.id.recycler_feedings)).check(matches(isDisplayed()))
  }

  @Test
  fun criticalPath_childDetailThenDiapersList() {
    doLogin()
    openFirstChild()

    onView(withId(R.id.button_diapers)).perform(click())

    onView(withId(R.id.recycler_diapers)).check(matches(isDisplayed()))
  }

  @Test
  fun criticalPath_childDetailThenNapsList() {
    doLogin()
    openFirstChild()

    onView(withId(R.id.button_naps)).perform(click())

    onView(withId(R.id.recycler_naps)).check(matches(isDisplayed()))
  }

  @Test
  fun criticalPath_childDetailThenSharing() {
    doLogin()
    openFirstChild()

    onView(withId(R.id.button_share)).perform(click())

    onView(withId(R.id.recycler_sharing)).check(matches(isDisplayed()))
  }
}

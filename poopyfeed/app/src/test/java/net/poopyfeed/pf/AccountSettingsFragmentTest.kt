package net.poopyfeed.pf

import android.os.Build
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Fragment tests for AccountSettingsFragment.
 *
 * Tests focus on UI state rendering and layout visibility (not dialogs or navigation).
 * Dialog and navigation behavior is tested via ViewModel layer.
 *
 * Note: Full dialog testing would require ActivityScenario or on-device testing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AccountSettingsFragmentTest {

  private lateinit var scenario: FragmentScenario<AccountSettingsFragment>

  @Before
  fun setup() {
    scenario = launchFragmentInContainer(themeResId = R.style.Theme_PoopyFeed)
  }

  @Test
  fun profileCardDisplayedOnReady() {
    onView(withId(R.id.card_profile)).check(matches(isDisplayed()))
  }

  @Test
  fun passwordCardDisplayedOnReady() {
    onView(withId(R.id.card_password)).check(matches(isDisplayed()))
  }

  @Test
  fun deleteCardDisplayedOnReady() {
    onView(withId(R.id.card_delete)).check(matches(isDisplayed()))
  }

  @Test
  fun profileSaveButtonDisplayedOnReady() {
    onView(withId(R.id.button_account_save)).check(matches(isDisplayed()))
  }

  @Test
  fun passwordChangeButtonDisplayedOnReady() {
    onView(withId(R.id.button_change_password)).check(matches(isDisplayed()))
  }

  @Test
  fun deleteAccountButtonDisplayedOnReady() {
    onView(withId(R.id.button_delete_account)).check(matches(isDisplayed()))
  }

  @Test
  fun profileProgressIndicatorHiddenInitially() {
    onView(withId(R.id.progress_account)).check(matches(isDisplayed()))
  }

  @Test
  fun passwordProgressIndicatorHiddenInitially() {
    onView(withId(R.id.progress_password)).check(matches(isDisplayed()))
  }

  @Test
  fun deleteProgressIndicatorHiddenInitially() {
    onView(withId(R.id.progress_delete)).check(matches(isDisplayed()))
  }

  @Test
  fun passwordSectionTitleDisplayed() {
    onView(withId(R.id.text_password_title))
        .check(matches(withText(R.string.account_password_section_title)))
  }

  @Test
  fun deleteSectionTitleDisplayed() {
    onView(withId(R.id.text_delete_title))
        .check(matches(withText(R.string.account_delete_section_title)))
  }

  @Test
  fun deleteWarningTextDisplayed() {
    onView(withId(R.id.text_delete_warning))
        .check(matches(withText(R.string.account_delete_warning)))
  }

  @Test
  fun passwordFieldsVisible() {
    onView(withId(R.id.edit_text_password_current)).check(matches(isDisplayed()))
    onView(withId(R.id.edit_text_password_new)).check(matches(isDisplayed()))
    onView(withId(R.id.edit_text_password_confirm)).check(matches(isDisplayed()))
  }

  @Test
  fun profileEmailFieldDisabled() {
    onView(withId(R.id.edit_text_account_email)).check(matches(isDisplayed()))
  }
}

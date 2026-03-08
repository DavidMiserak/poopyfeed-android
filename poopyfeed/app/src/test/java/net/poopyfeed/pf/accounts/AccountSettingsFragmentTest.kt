package net.poopyfeed.pf.accounts

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.UserProfileUpdate
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [AccountSettingsFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class AccountSettingsFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val authRepository: AuthRepository = mockk(relaxed = true)

  @BindValue @JvmField val tokenManager: TokenManager = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext())
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    every { tokenManager.getToken() } returns "token"
    coEvery { authRepository.getProfile() } returns
        ApiResult.Success(
            TestFixtures.mockUserProfile(
                email = "user@example.com",
                first_name = "Jane",
                last_name = "Doe",
                timezone = "America/New_York",
            ),
        )
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.AccountSettingsFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): AccountSettingsFragment {
    var fragment: AccountSettingsFragment? = null
    launchFragmentInHiltContainer<AccountSettingsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `ready state populates profile fields`() {
    val fragment = launchFragment()
    val root = fragment.requireView()

    assertEquals(
        "user@example.com",
        root.findViewById<android.widget.EditText>(R.id.edit_text_account_email).text.toString(),
    )
    assertEquals(
        "Jane",
        root
            .findViewById<android.widget.EditText>(R.id.edit_text_account_first_name)
            .text
            .toString(),
    )
    assertEquals(
        "Doe",
        root
            .findViewById<android.widget.EditText>(R.id.edit_text_account_last_name)
            .text
            .toString(),
    )
    assertNotNull(
        root
            .findViewById<android.widget.AutoCompleteTextView>(R.id.auto_complete_account_timezone)
            .text
            .toString(),
    )
  }

  @Test
  fun `save profile calls updateProfile`() {
    val updatedProfile =
        TestFixtures.mockUserProfile(
            email = "user@example.com",
            first_name = "Jane",
            last_name = "Smith",
            timezone = "America/Los_Angeles",
        )
    coEvery {
      authRepository.updateProfile(UserProfileUpdate("Jane", "Smith", "America/Los_Angeles"))
    } returns ApiResult.Success(updatedProfile)

    val fragment = launchFragment()
    val root = fragment.requireView()

    root.findViewById<android.widget.EditText>(R.id.edit_text_account_last_name).setText("Smith")
    root
        .findViewById<android.widget.AutoCompleteTextView>(R.id.auto_complete_account_timezone)
        .setText("America/Los_Angeles")
    root.findViewById<View>(R.id.button_account_save).performClick()
    repeat(40) { ShadowLooper.idleMainLooper() }

    coVerify {
      authRepository.updateProfile(
          UserProfileUpdate(
              first_name = "Jane",
              last_name = "Smith",
              timezone = "America/Los_Angeles",
          ),
      )
    }
  }

  @Test
  fun `unauthorized navigates to LoginFragment`() {
    every { tokenManager.getToken() } returns null

    var fragment: AccountSettingsFragment? = null
    launchFragmentInHiltContainer<AccountSettingsFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(40) { ShadowLooper.idleMainLooper() }

    assertEquals(R.id.LoginFragment, navController.currentDestination?.id)
  }
}

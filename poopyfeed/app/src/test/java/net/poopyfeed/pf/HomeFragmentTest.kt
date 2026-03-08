package net.poopyfeed.pf

import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** UI tests for [HomeFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val authRepository: AuthRepository = mockk(relaxed = true)

  @BindValue @JvmField val tokenManager: TokenManager = mockk(relaxed = true)

  private lateinit var navController: TestNavHostController

  @Before
  fun setup() {
    WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext())
    hiltRule.inject()
    navController = TestNavHostController(ApplicationProvider.getApplicationContext())
  }

  private fun installNavController(activity: android.app.Activity) {
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(R.id.HomeFragment)
    Navigation.setViewNavController(activity.findViewById(android.R.id.content), navController)
  }

  private fun launchFragment(): HomeFragment {
    var fragment: HomeFragment? = null
    launchFragmentInHiltContainer<HomeFragment>(beforeAdd = ::installNavController) {
      fragment = this
    }
    repeat(20) { ShadowLooper.idleMainLooper() }
    return fragment!!
  }

  @Test
  fun `no token navigates to LoginFragment`() {
    every { tokenManager.getToken() } returns null

    launchFragment()
    repeat(10) { ShadowLooper.idleMainLooper() }

    assertEquals(R.id.LoginFragment, navController.currentDestination?.id)
  }

  @Test
  fun `has token and profile ready shows welcome with email`() {
    every { tokenManager.getToken() } returns "token"
    coEvery { authRepository.getProfile() } returns
        ApiResult.Success(TestFixtures.mockUserProfile(email = "sarah@example.com"))

    val fragment = launchFragment()
    repeat(40) { ShadowLooper.idleMainLooper() }

    val welcomeText =
        fragment
            .requireView()
            .findViewById<android.widget.TextView>(R.id.text_welcome)
            .text
            .toString()
    assertTrue(
        "Welcome text should contain profile email",
        welcomeText.contains("sarah@example.com"),
    )
  }

  @Test
  fun `my children card click navigates to ChildrenListFragment`() {
    every { tokenManager.getToken() } returns "token"
    coEvery { authRepository.getProfile() } returns
        ApiResult.Success(TestFixtures.mockUserProfile())

    val fragment = launchFragment()
    repeat(40) { ShadowLooper.idleMainLooper() }

    fragment.requireView().findViewById<View>(R.id.card_my_children).performClick()
    repeat(5) { ShadowLooper.idleMainLooper() }

    assertEquals(R.id.ChildrenListFragment, navController.currentDestination?.id)
  }
}

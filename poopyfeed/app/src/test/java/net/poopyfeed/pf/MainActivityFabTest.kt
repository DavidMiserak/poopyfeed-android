package net.poopyfeed.pf

import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import net.poopyfeed.pf.di.TokenManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Activity-level tests for FAB behavior on child detail (quick log). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MainActivityFabTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val tokenManager: TokenManager = mockk(relaxed = true)

  @Before
  fun setup() {
    WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext())
    hiltRule.inject()
    // Avoid timezone banner behavior depending on profile timezone.
    every { tokenManager.getProfileTimezone() } returns null
  }

  @Test
  fun `fab on ChildDetail navigates to quick log sheet with childId`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()

    // Wait until NavController is ready.
    idleMainLooperUntil {
      activity.findViewById<View>(R.id.nav_host_fragment_content_main) != null &&
          runCatching { activity.findNavController(R.id.nav_host_fragment_content_main) }
              .getOrNull()
              ?.currentDestination != null
    }

    val navController = activity.findNavController(R.id.nav_host_fragment_content_main)

    // Navigate to ChildDetailFragment with a specific childId.
    val childId = 42
    navController.navigate(
        R.id.ChildDetailFragment,
        Bundle().apply { putInt("childId", childId) },
    )
    idleMainLooperUntil { navController.currentDestination?.id == R.id.ChildDetailFragment }

    // Click the global FAB.
    val fab = activity.findViewById<FloatingActionButton>(R.id.fab)
    fab.performClick()

    // FAB should navigate to the quick log sheet, carrying the same childId argument.
    idleMainLooperUntil { navController.currentDestination?.id == R.id.childDetailQuickLogSheet }

    assertEquals(R.id.childDetailQuickLogSheet, navController.currentDestination?.id)
    val args = navController.currentBackStackEntry?.arguments
    assertEquals(childId, args?.getInt("childId"))
  }
}

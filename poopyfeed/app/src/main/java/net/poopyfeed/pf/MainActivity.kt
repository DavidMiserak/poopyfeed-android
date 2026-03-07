package net.poopyfeed.pf

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.poopyfeed.pf.databinding.ActivityMainBinding

/**
 * Single-activity host for the app. Hosts the NavHostFragment, shows/hides AppBar and FAB based on
 * destination (hidden on Login/Signup). Handles logout and navigation to Login with a clean back
 * stack.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding
  private lateinit var appBarConfiguration: AppBarConfiguration

  private val viewModel: MainActivityViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DynamicColors.applyToActivityIfAvailable(this)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)

    binding.buttonUseDeviceTimezone.setOnClickListener { viewModel.useDeviceTimezone() }
    binding.buttonDismissTimezoneBanner.setOnClickListener { viewModel.dismissTimezoneBanner() }

    setupLifecycleObservers()
    setupNavigation()
  }

  private fun setupLifecycleObservers() {
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.checkTimezoneMismatch()
        viewModel.refreshUnreadCount()
        launch { viewModel.logoutNavigateToLogin.collect { navigateToLoginAfterLogout() } }
        launch { viewModel.timezoneBanner.collect { bindTimezoneBannerState(it) } }
        launch {
          viewModel.bannerError.collect { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
          }
        }
        launch {
          viewModel.unreadCount.collect { count ->
            val badge = binding.bottomNav.getOrCreateBadge(R.id.NotificationsFragment)
            badge.number = count
            badge.isVisible = count > 0
          }
        }
        launch {
          while (true) {
            delay(30_000)
            viewModel.refreshUnreadCount()
          }
        }
      }
    }
  }

  private fun bindTimezoneBannerState(state: TimezoneBannerState) {
    val saving = state is TimezoneBannerState.Saving
    binding.bannerTimezoneMismatch.isVisible = state !is TimezoneBannerState.Hidden
    binding.progressTimezoneBanner.isVisible = saving
    binding.buttonUseDeviceTimezone.isEnabled = !saving

    if (state is TimezoneBannerState.Visible || state is TimezoneBannerState.Saving) {
      val deviceTz =
          when (state) {
            is TimezoneBannerState.Visible -> state.deviceTimezone
            is TimezoneBannerState.Saving -> state.deviceTimezone
          }
      val profileTz =
          when (state) {
            is TimezoneBannerState.Visible -> state.profileTimezone
            is TimezoneBannerState.Saving -> state.profileTimezone
          }
      binding.textTimezoneMismatch.text =
          getString(R.string.timezone_mismatch_message, deviceTz, profileTz)
    }
  }

  private fun setupNavigation() {
    binding.root.findViewById<View>(R.id.nav_host_fragment_content_main).post {
      val navController = findNavController(R.id.nav_host_fragment_content_main)
      appBarConfiguration =
          AppBarConfiguration(
              setOf(R.id.HomeFragment, R.id.ChildrenListFragment, R.id.NotificationsFragment))
      setupActionBarWithNavController(navController, appBarConfiguration)
      NavigationUI.setupWithNavController(binding.bottomNav, navController)
      navController.addOnDestinationChangedListener { _, destination, _ ->
        onDestinationChanged(navController, destination)
      }
    }
  }

  private fun onDestinationChanged(
      navController: androidx.navigation.NavController,
      destination: androidx.navigation.NavDestination
  ) {
    val isAuthDestination =
        destination.id == R.id.LoginFragment || destination.id == R.id.SignupFragment
    binding.appBar.isVisible = !isAuthDestination
    binding.bottomNav.isVisible = !isAuthDestination

    // Show FAB only on list screens (children, feedings, diapers, naps)
    val isFabDestination =
        destination.id in
            setOf(
                R.id.ChildrenListFragment,
                R.id.FeedingsListFragment,
                R.id.DiapersListFragment,
                R.id.NapsListFragment)
    binding.fab.isVisible = isFabDestination

    binding.fab.setOnClickListener {
      when (destination.id) {
        R.id.ChildrenListFragment -> {
          navController.navigate(R.id.createChildBottomSheet)
        }
        R.id.FeedingsListFragment -> {
          val childId =
              navController.currentBackStackEntry?.arguments?.getInt("childId")
                  ?: return@setOnClickListener
          navController.navigate(
              R.id.action_feedingsList_to_createFeeding,
              Bundle().apply { putInt("childId", childId) })
        }
        R.id.DiapersListFragment -> {
          val childId =
              navController.currentBackStackEntry?.arguments?.getInt("childId")
                  ?: return@setOnClickListener
          navController.navigate(
              R.id.action_diapersList_to_createDiaper,
              Bundle().apply { putInt("childId", childId) })
        }
        R.id.NapsListFragment -> {
          val childId =
              navController.currentBackStackEntry?.arguments?.getInt("childId")
                  ?: return@setOnClickListener
          navController.navigate(
              R.id.action_napsList_to_createNap, Bundle().apply { putInt("childId", childId) })
        }
      }
    }

    if (!isAuthDestination) {
      viewModel.checkTimezoneMismatch()
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp() || super.onSupportNavigateUp()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_account -> {
        findNavController(R.id.nav_host_fragment_content_main)
            .navigate(R.id.AccountSettingsFragment)
        true
      }
      R.id.action_logout -> {
        performLogout()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun performLogout() {
    viewModel.logout()
  }

  private fun navigateToLoginAfterLogout() {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
    navController.navigate(R.id.LoginFragment, null, navOptions)
  }
}

package net.poopyfeed.pf

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint
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

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.logoutNavigateToLogin.collect { navigateToLoginAfterLogout() }
      }
    }

    // FragmentContainerView creates NavHostFragment asynchronously; defer nav setup
    // until the host fragment exists (see FragmentTagUsage → FragmentContainerView).
    binding.root.findViewById<View>(R.id.nav_host_fragment_content_main).post {
      val navController = findNavController(R.id.nav_host_fragment_content_main)
      appBarConfiguration = AppBarConfiguration(setOf(R.id.HomeFragment))
      setupActionBarWithNavController(navController, appBarConfiguration)

      navController.addOnDestinationChangedListener { _, destination, _ ->
        val isAuthDestination =
            destination.id == R.id.LoginFragment || destination.id == R.id.SignupFragment
        binding.appBar.visibility = if (isAuthDestination) View.GONE else View.VISIBLE
        binding.fab.visibility = if (isAuthDestination) View.GONE else View.VISIBLE
      }
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
      R.id.action_settings -> {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.navigate(R.id.AccountSettingsFragment)
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

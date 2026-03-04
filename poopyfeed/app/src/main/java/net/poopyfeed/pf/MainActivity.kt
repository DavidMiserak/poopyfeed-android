package net.poopyfeed.pf

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.databinding.ActivityMainBinding
import net.poopyfeed.pf.di.NetworkModule

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

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
        lifecycleScope.launch {
            val apiService = NetworkModule.providePoopyFeedApiService(this@MainActivity)
            val authRepository = AuthRepository(apiService)

            when (authRepository.logout()) {
                is ApiResult.Success,
                is ApiResult.Loading -> {
                    // ignore
                }

                is ApiResult.Error -> {
                    // ignore errors, we'll still clear locally
                }
            }

            NetworkModule.clearAuthToken(this@MainActivity)

            val navController = findNavController(R.id.nav_host_fragment_content_main)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()

            navController.navigate(R.id.LoginFragment, null, navOptions)
        }
    }
}

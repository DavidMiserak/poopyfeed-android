package net.poopyfeed.pf

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import net.poopyfeed.pf.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isLogin = destination.id == R.id.LoginFragment
            binding.appBar.visibility = if (isLogin) View.GONE else View.VISIBLE
            binding.fab.visibility = if (isLogin) View.GONE else View.VISIBLE
        }
    }
}

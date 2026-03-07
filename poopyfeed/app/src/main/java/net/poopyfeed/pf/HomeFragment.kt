package net.poopyfeed.pf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.databinding.FragmentHomeBinding

/**
 * Home screen shown after login. Loads profile via [HomeViewModel]; shows email when ready or
 * navigates back to login if unauthorized. Placeholder for future children list and tracking.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: HomeViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (!viewModel.hasToken()) {
      navigateToLogin()
      return
    }

    // Setup "My Children" card click listener
    binding.cardMyChildren.setOnClickListener {
      findNavController().navigate(R.id.action_homeFragment_to_childrenList)
    }

    // Pending invites card: visibility and click to Pending Invites screen
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.pendingInvites.collect { invites ->
          binding.cardPendingInvites.visibility =
              if (invites.isNotEmpty()) View.VISIBLE else View.GONE
          if (invites.isNotEmpty()) {
            binding.textPendingInvitesTitle.text =
                getString(R.string.pending_invites_card_title, invites.size)
          }
        }
      }
    }
    binding.cardPendingInvites.setOnClickListener {
      findNavController().navigate(R.id.action_homeFragment_to_pendingInvitesFragment)
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is HomeUiState.Loading -> {
              // optional: show loading UI
            }
            is HomeUiState.Ready -> {
              binding.textWelcome.text = getString(R.string.welcome_message, state.email)
            }
            is HomeUiState.Unauthorized -> {
              navigateToLogin()
            }
            is HomeUiState.Error -> {
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
          }
        }
      }
    }
  }

  private fun navigateToLogin() {
    val navController = findNavController()
    navController.navigate(R.id.action_homeFragment_to_loginFragment)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

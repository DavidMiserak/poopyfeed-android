package net.poopyfeed.pf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.databinding.FragmentHomeBinding
import net.poopyfeed.pf.di.NetworkModule

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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

        val token = NetworkModule.getAuthToken(requireContext())
        if (token == null) {
            navigateToLogin()
            return
        }

        val apiService = NetworkModule.providePoopyFeedApiService(requireContext())
        val authRepository = AuthRepository(apiService)

        binding.buttonLogout.setOnClickListener {
            performLogout(authRepository)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.getProfile()) {
                is ApiResult.Loading -> {
                    // no-op
                }

                is ApiResult.Success -> {
                    val profile = result.data
                    binding.textWelcome.text = getString(
                        R.string.welcome_message,
                        profile.first_name,
                        profile.last_name
                    )
                }

                is ApiResult.Error -> {
                    val error = result.error
                    if (error is ApiError.HttpError && error.statusCode == 401) {
                        NetworkModule.clearAuthToken(requireContext())
                        navigateToLogin()
                    } else {
                        Snackbar.make(
                            binding.root,
                            error.getUserMessage(),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun performLogout(authRepository: AuthRepository) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (authRepository.logout()) {
                is ApiResult.Success,
                is ApiResult.Loading -> {
                    // ignore
                }

                is ApiResult.Error -> {
                    // ignore errors, we'll still clear locally
                }
            }
            NetworkModule.clearAuthToken(requireContext())
            navigateToLogin()
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

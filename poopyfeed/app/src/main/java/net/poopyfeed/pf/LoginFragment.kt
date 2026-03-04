package net.poopyfeed.pf

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.databinding.FragmentLoginBinding
import net.poopyfeed.pf.di.NetworkModule

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val existingToken = NetworkModule.getAuthToken(requireContext())
        if (existingToken != null) {
            navigateToHome()
            return
        }

        binding.buttonLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val email = binding.editTextEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editTextPassword.text?.toString().orEmpty()

        var hasError = false

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.error = getString(R.string.login_email_error)
            hasError = true
        } else {
            binding.inputLayoutEmail.error = null
        }

        if (password.isEmpty()) {
            binding.inputLayoutPassword.error = getString(R.string.login_password_error)
            hasError = true
        } else {
            binding.inputLayoutPassword.error = null
        }

        if (hasError) return

        val apiService = NetworkModule.providePoopyFeedApiService(requireContext())
        val authRepository = AuthRepository(apiService)

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.login(email, password)) {
                is ApiResult.Loading -> {
                    // no-op, we manage loading state locally
                }

                is ApiResult.Success -> {
                    NetworkModule.saveAuthToken(requireContext(), result.data)
                    setLoading(false)
                    navigateToHome()
                }

                is ApiResult.Error -> {
                    setLoading(false)
                    Snackbar.make(
                        binding.root,
                        result.error.getUserMessage(),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.buttonLogin.isEnabled = !loading
        binding.progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun navigateToHome() {
        val navController = findNavController()
        navController.navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

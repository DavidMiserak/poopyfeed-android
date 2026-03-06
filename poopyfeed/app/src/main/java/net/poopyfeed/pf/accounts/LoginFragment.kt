package net.poopyfeed.pf.accounts

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
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentLoginBinding
import net.poopyfeed.pf.util.EmailValidator

/**
 * Login screen. Collects email/password, validates with [EmailValidator], and delegates auth to
 * [LoginViewModel]. Navigates to [HomeFragment] on success or to signup. Uses
 * [repeatOnLifecycle][Lifecycle.State.STARTED] to collect [LoginViewModel.uiState].
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

  private var _binding: FragmentLoginBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: LoginViewModel by viewModels()

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

    if (viewModel.checkExistingToken()) {
      navigateToHome()
      return
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is LoginUiState.Idle -> {
              setLoading(false)
            }
            is LoginUiState.Loading -> {
              setLoading(true)
            }
            is LoginUiState.Success -> {
              setLoading(false)
              navigateToHome()
            }
            is LoginUiState.Error -> {
              setLoading(false)
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              viewModel.clearError()
            }
          }
        }
      }
    }

    binding.buttonLogin.setOnClickListener { attemptLogin() }

    binding.textGoToSignup.setOnClickListener {
      val navController = findNavController()
      navController.navigate(R.id.action_loginFragment_to_signupFragment)
    }
  }

  private fun attemptLogin() {
    val email = binding.editTextEmail.text?.toString()?.trim().orEmpty()
    val password = binding.editTextPassword.text?.toString().orEmpty()

    var hasError = false

    if (!EmailValidator.isValid(email)) {
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

    viewModel.login(email, password)
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

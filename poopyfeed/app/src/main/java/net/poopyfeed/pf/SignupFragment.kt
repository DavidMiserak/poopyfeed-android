package net.poopyfeed.pf

import android.os.Bundle
import android.util.Patterns
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
import kotlinx.coroutines.launch
import net.poopyfeed.pf.databinding.FragmentSignupBinding
import net.poopyfeed.pf.di.NetworkModule

class SignupFragment : Fragment() {

  private var _binding: FragmentSignupBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: SignupViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSignupBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is SignupUiState.Idle -> {
              setLoading(false)
            }
            is SignupUiState.Loading -> {
              setLoading(true)
            }
            is SignupUiState.Success -> {
              setLoading(false)
              val context = requireContext().applicationContext
              NetworkModule.saveAuthToken(context, state.token)
              navigateToHome()
            }
            is SignupUiState.Error -> {
              setLoading(false)
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              viewModel.clearError()
            }
          }
        }
      }
    }

    binding.buttonSignup.setOnClickListener { attemptSignup() }

    binding.textGoToLogin.setOnClickListener { findNavController().navigateUp() }
  }

  private fun attemptSignup() {
    val email = binding.editTextSignupEmail.text?.toString()?.trim().orEmpty()
    val password = binding.editTextSignupPassword.text?.toString().orEmpty()
    val confirmPassword = binding.editTextSignupConfirmPassword.text?.toString().orEmpty()

    var hasError = false

    if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      binding.inputLayoutSignupEmail.error = getString(R.string.signup_email_error)
      hasError = true
    } else {
      binding.inputLayoutSignupEmail.error = null
    }

    if (password.length < 8) {
      binding.inputLayoutSignupPassword.error = getString(R.string.signup_password_error)
      hasError = true
    } else {
      binding.inputLayoutSignupPassword.error = null
    }

    if (confirmPassword != password) {
      binding.inputLayoutSignupConfirmPassword.error =
          getString(R.string.signup_confirm_password_error)
      hasError = true
    } else {
      binding.inputLayoutSignupConfirmPassword.error = null
    }

    if (hasError) return

    viewModel.signUp(email, password)
  }

  private fun setLoading(loading: Boolean) {
    binding.buttonSignup.isEnabled = !loading
    binding.progressSignup.visibility = if (loading) View.VISIBLE else View.GONE
  }

  private fun navigateToHome() {
    val navController = findNavController()
    navController.navigate(R.id.action_signupFragment_to_homeFragment)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

package net.poopyfeed.pf

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.databinding.FragmentAccountSettingsBinding

/**
 * Account settings screen. Displays and edits user profile (name, email, timezone) via
 * [AccountSettingsViewModel]. Shown from the main toolbar menu.
 */
@AndroidEntryPoint
class AccountSettingsFragment : Fragment() {

  private var _binding: FragmentAccountSettingsBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: AccountSettingsViewModel by viewModels()

  private var hasPopulatedFields: Boolean = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // SECTION 1: Profile - Save button
    binding.buttonAccountSave.setOnClickListener {
      val firstName = binding.editTextAccountFirstName.text?.toString()?.trim().orEmpty()
      val lastName = binding.editTextAccountLastName.text?.toString()?.trim().orEmpty()
      val timezone = binding.autoCompleteAccountTimezone.text?.toString()?.trim().orEmpty()

      viewModel.saveProfile(firstName, lastName, timezone)
    }

    // SECTION 2: Security - Change password button (shows confirmation dialog)
    binding.buttonChangePassword.setOnClickListener { showPasswordChangeDialog() }

    // SECTION 3: Danger Zone - Delete account button (shows confirmation dialog)
    binding.buttonDeleteAccount.setOnClickListener { showDeleteAccountDialog() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            // Profile states
            is AccountSettingsUiState.Loading -> {
              setProfileLoading(true)
            }
            is AccountSettingsUiState.Saving -> {
              setProfileLoading(true)
            }
            is AccountSettingsUiState.Ready -> {
              setProfileLoading(false)
              setPasswordLoading(false)
              setDeleteLoading(false)
              if (!hasPopulatedFields) {
                bindProfile(state.profile, state.timezones)
                hasPopulatedFields = true
              }
            }
            is AccountSettingsUiState.Saved -> {
              setProfileLoading(false)
              Snackbar.make(
                      binding.root, getString(R.string.account_saved_message), Snackbar.LENGTH_LONG)
                  .show()
              hasPopulatedFields = false
              bindProfile(state.profile, state.timezones)
            }
            is AccountSettingsUiState.Error -> {
              setProfileLoading(false)
              Snackbar.make(
                      binding.root,
                      state.message.ifBlank { getString(R.string.account_load_error) },
                      Snackbar.LENGTH_LONG)
                  .show()
            }

            // Password change states
            is AccountSettingsUiState.ChangingPassword -> {
              setPasswordLoading(true)
              binding.buttonChangePassword.isEnabled = false
            }
            is AccountSettingsUiState.PasswordChanged -> {
              setPasswordLoading(false)
              binding.buttonChangePassword.isEnabled = true
              Snackbar.make(
                      binding.root,
                      getString(R.string.account_password_changed_message),
                      Snackbar.LENGTH_LONG)
                  .show()
              clearPasswordFields()
              viewModel.clearPasswordChangeState()
            }
            is AccountSettingsUiState.PasswordChangeError -> {
              setPasswordLoading(false)
              binding.buttonChangePassword.isEnabled = true
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }

            // Account deletion states
            is AccountSettingsUiState.DeletingAccount -> {
              setDeleteLoading(true)
              binding.buttonDeleteAccount.isEnabled = false
            }
            is AccountSettingsUiState.AccountDeleted -> {
              setDeleteLoading(false)
              Snackbar.make(
                      binding.root,
                      getString(R.string.account_deleted_message),
                      Snackbar.LENGTH_LONG)
                  .show()
              // Navigate to login after brief delay
              view?.postDelayed(
                  {
                    viewModel.clearDeletionState()
                    navigateToLogin()
                  },
                  1500)
            }
            is AccountSettingsUiState.DeletionError -> {
              setDeleteLoading(false)
              binding.buttonDeleteAccount.isEnabled = true
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }

            // Unauthorized (token expired)
            is AccountSettingsUiState.Unauthorized -> {
              setProfileLoading(false)
              navigateToLogin()
            }
          }
        }
      }
    }
  }

  private fun bindProfile(profile: UserProfile, timezones: List<String>) {
    binding.editTextAccountEmail.setText(profile.email)
    binding.editTextAccountFirstName.setText(profile.first_name)
    binding.editTextAccountLastName.setText(profile.last_name)
    binding.autoCompleteAccountTimezone.setText(profile.timezone, false)

    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, timezones)
    binding.autoCompleteAccountTimezone.setAdapter(adapter)
  }

  private fun setProfileLoading(loading: Boolean) {
    binding.buttonAccountSave.isEnabled = !loading
    binding.progressAccount.visibility = if (loading) View.VISIBLE else View.GONE
  }

  private fun setPasswordLoading(loading: Boolean) {
    binding.buttonChangePassword.isEnabled = !loading
    binding.progressPassword.visibility = if (loading) View.VISIBLE else View.GONE
  }

  private fun setDeleteLoading(loading: Boolean) {
    binding.buttonDeleteAccount.isEnabled = !loading
    binding.progressDelete.visibility = if (loading) View.VISIBLE else View.GONE
  }

  private fun clearPasswordFields() {
    binding.editTextPasswordCurrent.text?.clear()
    binding.editTextPasswordNew.text?.clear()
    binding.editTextPasswordConfirm.text?.clear()
    clearPasswordFieldErrors()
  }

  private fun clearPasswordFieldErrors() {
    binding.inputLayoutPasswordCurrent.error = null
    binding.inputLayoutPasswordNew.error = null
    binding.inputLayoutPasswordConfirm.error = null
  }

  private fun showPasswordChangeDialog() {
    val currentPassword = binding.editTextPasswordCurrent.text?.toString().orEmpty()
    val newPassword = binding.editTextPasswordNew.text?.toString().orEmpty()
    val confirmPassword = binding.editTextPasswordConfirm.text?.toString().orEmpty()

    // Validate before showing dialog
    var hasErrors = false
    clearPasswordFieldErrors()

    if (currentPassword.isBlank()) {
      binding.inputLayoutPasswordCurrent.error =
          getString(R.string.account_current_password_label) + " required"
      hasErrors = true
    }
    if (newPassword.isBlank()) {
      binding.inputLayoutPasswordNew.error =
          getString(R.string.account_new_password_label) + " required"
      hasErrors = true
    } else if (newPassword.length < 8) {
      binding.inputLayoutPasswordNew.error =
          getString(R.string.account_password_validation_error_short)
      hasErrors = true
    }
    if (confirmPassword.isBlank()) {
      binding.inputLayoutPasswordConfirm.error =
          getString(R.string.account_confirm_password_label) + " required"
      hasErrors = true
    } else if (newPassword != confirmPassword) {
      binding.inputLayoutPasswordConfirm.error =
          getString(R.string.account_password_validation_error_mismatch)
      hasErrors = true
    }

    if (hasErrors) {
      Snackbar.make(
              binding.root,
              getString(R.string.account_password_validation_error_short),
              Snackbar.LENGTH_LONG)
          .show()
      return
    }

    // All validation passed, show confirmation dialog
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.account_password_change_dialog_title)
        .setMessage(R.string.account_password_change_dialog_message)
        .setPositiveButton(R.string.account_password_change_dialog_confirm) { _, _ ->
          viewModel.changePassword(currentPassword, newPassword, confirmPassword)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun showDeleteAccountDialog() {
    // Step 1: Initial warning dialog with password field
    val passwordInput =
        EditText(requireContext()).apply {
          inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
          hint = getString(R.string.account_current_password_label)
          setPadding(48, 32, 48, 32)
          textSize = 16f
        }

    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.account_delete_dialog_title)
        .setMessage(R.string.account_delete_dialog_message)
        .setView(passwordInput)
        .setPositiveButton(R.string.account_delete_confirm_button) { _, _ ->
          val password = passwordInput.text?.toString().orEmpty()
          if (password.isNotBlank()) {
            // Step 2: Final confirmation dialog (very explicit warning)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_delete_dialog_title)
                .setMessage(
                    getString(R.string.account_delete_warning) +
                        "\n\n" +
                        getString(R.string.account_delete_dialog_message))
                .setPositiveButton(R.string.account_delete_confirm_button) { _, _ ->
                  viewModel.deleteAccount(password)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show()
          } else {
            Snackbar.make(
                    binding.root,
                    getString(R.string.account_current_password_label) + " required",
                    Snackbar.LENGTH_LONG)
                .show()
          }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun navigateToLogin() {
    val navController = findNavController()
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
    navController.navigate(R.id.LoginFragment, null, navOptions)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

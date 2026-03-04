package net.poopyfeed.pf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.databinding.FragmentAccountSettingsBinding

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

    binding.buttonAccountSave.setOnClickListener {
      val firstName = binding.editTextAccountFirstName.text?.toString()?.trim().orEmpty()
      val lastName = binding.editTextAccountLastName.text?.toString()?.trim().orEmpty()
      val timezone = binding.autoCompleteAccountTimezone.text?.toString()?.trim().orEmpty()

      viewModel.saveProfile(firstName, lastName, timezone)
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is AccountSettingsUiState.Loading -> {
              setLoading(true)
            }
            is AccountSettingsUiState.Saving -> {
              setLoading(true)
            }
            is AccountSettingsUiState.Ready -> {
              setLoading(false)
              if (!hasPopulatedFields) {
                bindProfile(state.profile, state.timezones)
                hasPopulatedFields = true
              }
            }
            is AccountSettingsUiState.Saved -> {
              setLoading(false)
              Snackbar.make(
                      binding.root, getString(R.string.account_saved_message), Snackbar.LENGTH_LONG)
                  .show()
              hasPopulatedFields = false
              bindProfile(state.profile, state.timezones)
            }
            is AccountSettingsUiState.Error -> {
              setLoading(false)
              Snackbar.make(
                      binding.root,
                      state.message.ifBlank { getString(R.string.account_load_error) },
                      Snackbar.LENGTH_LONG)
                  .show()
            }
            is AccountSettingsUiState.Unauthorized -> {
              setLoading(false)
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

  private fun setLoading(loading: Boolean) {
    binding.buttonAccountSave.isEnabled = !loading
    binding.progressAccount.visibility = if (loading) View.VISIBLE else View.GONE
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

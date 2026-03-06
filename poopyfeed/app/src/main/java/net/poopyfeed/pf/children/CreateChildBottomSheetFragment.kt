package net.poopyfeed.pf.children

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentCreateChildBottomSheetBinding

/**
 * Bottom sheet fragment for creating a new child. Allows user to enter name, date of birth, and
 * gender. Validates inputs and calls the API to create the child.
 */
@AndroidEntryPoint
class CreateChildBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateChildBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: CreateChildViewModel by viewModels()
  private var selectedDate: String? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCreateChildBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Setup DOB field click to show date picker
    binding.inputDob.setOnClickListener { showDatePicker() }

    // Setup save button click
    binding.buttonSave.setOnClickListener { saveChild() }

    // Collect UI state
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is CreateChildUiState.Idle -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is CreateChildUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is CreateChildUiState.Success -> {
              Snackbar.make(binding.root, R.string.create_child_success, Snackbar.LENGTH_SHORT)
                  .show()
              // Signal success via SavedStateHandle
              findNavController()
                  .previousBackStackEntry
                  ?.savedStateHandle
                  ?.set("child_created", true)
              dismiss()
            }
            is CreateChildUiState.Error -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is CreateChildUiState.ValidationError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              // Show validation errors
              binding.layoutName.error =
                  state.nameError ?: getString(R.string.create_child_name_error)
              binding.layoutDob.error = state.dobError ?: getString(R.string.create_child_dob_error)
              // Note: RadioGroup doesn't have built-in error support
              if (state.genderError != null) {
                Snackbar.make(binding.root, state.genderError, Snackbar.LENGTH_SHORT).show()
              }
            }
          }
        }
      }
    }
  }

  private fun showDatePicker() {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
              val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
              val cal = Calendar.getInstance()
              cal.set(selectedYear, selectedMonth, selectedDay)
              selectedDate = formatter.format(cal.time)
              binding.inputDob.setText(selectedDate)
            },
            year,
            month,
            day)
        .show()
  }

  private fun saveChild() {
    val name = binding.inputName.text.toString().trim()
    val dob = selectedDate ?: ""
    val genderRadioId = binding.radioGender.checkedRadioButtonId
    val gender =
        when (genderRadioId) {
          R.id.radio_boy -> "M"
          R.id.radio_girl -> "F"
          else -> ""
        }

    viewModel.createChild(name, dob, gender)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

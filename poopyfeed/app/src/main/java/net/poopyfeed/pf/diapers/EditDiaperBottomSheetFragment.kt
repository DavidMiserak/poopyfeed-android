package net.poopyfeed.pf.diapers

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
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.databinding.FragmentCreateDiaperBottomSheetBinding
import net.poopyfeed.pf.util.formatTimestampForDisplay

/**
 * Bottom sheet for editing an existing diaper change. Reuses the create diaper form layout;
 * prefills change type and timestamp. On success signals diaper_updated and dismisses.
 */
@AndroidEntryPoint
class EditDiaperBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateDiaperBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: EditDiaperViewModel by viewModels()
  private var selectedTimestamp: String = ""
  private var formPrefilled: Boolean = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCreateDiaperBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.textSheetTitle.setText(R.string.edit_diaper_title)
    binding.buttonChangeTime.setOnClickListener { showDateTimePickers() }
    binding.buttonSave.setOnClickListener { saveDiaper() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is EditDiaperUiState.Loading -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditDiaperUiState.Ready -> {
              if (!formPrefilled) {
                bindForm(state.diaper)
                formPrefilled = true
              }
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is EditDiaperUiState.Error -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is EditDiaperUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditDiaperUiState.Success -> {
              findNavController()
                  .previousBackStackEntry
                  ?.savedStateHandle
                  ?.set("diaper_updated", true)
              dismiss()
            }
            is EditDiaperUiState.SaveError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is EditDiaperUiState.ValidationError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              state.typeError?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
            }
          }
        }
      }
    }
  }

  private fun bindForm(diaper: Diaper) {
    when (diaper.change_type.lowercase()) {
      "wet" -> binding.radioWet.isChecked = true
      "dirty" -> binding.radioDirty.isChecked = true
      "both" -> binding.radioBoth.isChecked = true
      else -> binding.radioChangeType.clearCheck()
    }
    selectedTimestamp = diaper.timestamp
    updateTimestampDisplay()
  }

  private fun updateTimestampDisplay() {
    binding.textTimestamp.text = formatTimestampForDisplay(requireContext(), selectedTimestamp)
  }

  private fun showDateTimePickers() {
    val cal = Calendar.getInstance()
    try {
      val instant = kotlinx.datetime.Instant.parse(selectedTimestamp)
      cal.timeInMillis = instant.toEpochMilliseconds()
    } catch (_: Exception) {}
    val datePicker =
        com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setSelection(cal.timeInMillis)
            .build()
    datePicker.addOnPositiveButtonClickListener { millis ->
      cal.timeInMillis = millis
      val timePicker =
          MaterialTimePicker.Builder()
              .setTimeFormat(TimeFormat.CLOCK_12H)
              .setHour(cal.get(Calendar.HOUR_OF_DAY))
              .setMinute(cal.get(Calendar.MINUTE))
              .build()
      timePicker.addOnPositiveButtonClickListener {
        cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        cal.set(Calendar.MINUTE, timePicker.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        selectedTimestamp =
            kotlinx.datetime.Instant.fromEpochMilliseconds(cal.timeInMillis).toString()
        updateTimestampDisplay()
      }
      timePicker.show(parentFragmentManager, "time")
    }
    datePicker.show(parentFragmentManager, "date")
  }

  private fun saveDiaper() {
    val changeType =
        when (binding.radioChangeType.checkedRadioButtonId) {
          R.id.radio_wet -> "wet"
          R.id.radio_dirty -> "dirty"
          R.id.radio_both -> "both"
          else -> ""
        }
    viewModel.saveDiaper(changeType, selectedTimestamp)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

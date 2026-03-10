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
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentCreateDiaperBottomSheetBinding
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.util.DatePickerUtils
import net.poopyfeed.pf.util.formatTimestampForDisplay

/**
 * Bottom sheet for logging a diaper change. Supports wet/dirty/both and optional timestamp. On
 * success signals diaper_created and dismisses.
 */
@AndroidEntryPoint
class CreateDiaperBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateDiaperBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: CreateDiaperViewModel by viewModels()
  @Inject lateinit var tokenManager: TokenManager
  private var selectedTimestamp: String = Clock.System.now().toString()

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
    updateTimestampDisplay()
    binding.buttonChangeTime.setOnClickListener { showDateTimePickers() }
    binding.buttonSave.setOnClickListener { saveDiaper() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is CreateDiaperUiState.Idle -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is CreateDiaperUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is CreateDiaperUiState.Success -> {
              findNavController()
                  .previousBackStackEntry
                  ?.savedStateHandle
                  ?.set("diaper_created", true)
              dismiss()
            }
            is CreateDiaperUiState.Error -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is CreateDiaperUiState.ValidationError -> {
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

  private fun updateTimestampDisplay() {
    binding.textTimestamp.text = formatTimestampForDisplay(requireContext(), selectedTimestamp)
  }

  private fun showDateTimePickers() {
    val tzId = tokenManager.getProfileTimezone()
    val initMillis = DatePickerUtils.datePickerSelectionMillis(selectedTimestamp, tzId)
    val (pickerHour, pickerMinute) = DatePickerUtils.timePickerHourMinute(selectedTimestamp, tzId)

    val datePicker =
        com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setSelection(initMillis)
            .build()
    datePicker.addOnPositiveButtonClickListener { millis ->
      val (year, month, day) = DatePickerUtils.extractSelectedDate(millis)
      val timePicker =
          MaterialTimePicker.Builder()
              .setTimeFormat(TimeFormat.CLOCK_12H)
              .setHour(pickerHour)
              .setMinute(pickerMinute)
              .build()
      timePicker.addOnPositiveButtonClickListener {
        selectedTimestamp =
            DatePickerUtils.toUtcIso(year, month, day, timePicker.hour, timePicker.minute, tzId)
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
    viewModel.createDiaper(changeType, selectedTimestamp)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

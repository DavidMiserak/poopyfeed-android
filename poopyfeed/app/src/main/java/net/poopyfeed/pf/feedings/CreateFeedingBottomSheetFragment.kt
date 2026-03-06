package net.poopyfeed.pf.feedings

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
import kotlinx.datetime.Clock
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentCreateFeedingBottomSheetBinding
import net.poopyfeed.pf.formatTimestampForDisplay

/**
 * Bottom sheet for logging a feeding. Supports bottle (with amount) and breast, with optional
 * timestamp. On success signals feeding_created and dismisses.
 */
@AndroidEntryPoint
class CreateFeedingBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateFeedingBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: CreateFeedingViewModel by viewModels()
  private var selectedTimestamp: String = Clock.System.now().toString()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCreateFeedingBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateTimestampDisplay()
    binding.radioFeedingType.setOnCheckedChangeListener { _, checkedId ->
      binding.layoutAmount.visibility =
          if (checkedId == R.id.radio_bottle) View.VISIBLE else View.GONE
    }
    binding.layoutAmount.visibility = if (binding.radioBottle.isChecked) View.VISIBLE else View.GONE
    binding.buttonChangeTime.setOnClickListener { showDateTimePickers() }
    binding.buttonSave.setOnClickListener { saveFeeding() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is CreateFeedingUiState.Idle -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is CreateFeedingUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is CreateFeedingUiState.Success -> {
              findNavController()
                  .previousBackStackEntry
                  ?.savedStateHandle
                  ?.set("feeding_created", true)
              dismiss()
            }
            is CreateFeedingUiState.Error -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is CreateFeedingUiState.ValidationError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              state.typeError?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
              binding.layoutAmount.error = state.amountError
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
    val cal = Calendar.getInstance()
    try {
      val instant = kotlinx.datetime.Instant.parse(selectedTimestamp)
      val millis = instant.toEpochMilliseconds()
      cal.timeInMillis = millis
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

  private fun saveFeeding() {
    val type =
        when (binding.radioFeedingType.checkedRadioButtonId) {
          R.id.radio_bottle -> "bottle"
          R.id.radio_breast -> "breast"
          else -> ""
        }
    val amountStr = binding.inputAmount.text?.toString()?.trim()
    val amount = amountStr?.toDoubleOrNull()
    viewModel.createFeeding(type, amount, selectedTimestamp)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

package net.poopyfeed.pf.naps

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
import net.poopyfeed.pf.databinding.FragmentCreateNapBottomSheetBinding
import net.poopyfeed.pf.util.formatTimestampForDisplay

/**
 * Bottom sheet for starting a nap. Start time defaults to now; user can change it. On success
 * signals nap_created and dismisses.
 */
@AndroidEntryPoint
class CreateNapBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateNapBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: CreateNapViewModel by viewModels()
  private var selectedTimestamp: String = Clock.System.now().toString()
  private var selectedEndTimestamp: String? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCreateNapBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    updateTimestampDisplay()
    updateEndTimeDisplay()
    updateEndTimeButtonLabel()
    validateForm() // Initial validation
    binding.buttonChangeTime.setOnClickListener { showStartDateTimePickers() }
    binding.buttonChangeEndTime.setOnClickListener { showEndDateTimePickers() }
    binding.buttonStartNap.setOnClickListener { startNap() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.uiState.collect { state ->
            when (state) {
              is CreateNapUiState.Idle -> {
                binding.buttonStartNap.isEnabled = isFormValid()
                binding.buttonStartNap.text = getString(R.string.create_nap_start_button)
                binding.buttonStartNap.visibility = View.VISIBLE
                binding.progressSaving.visibility = View.GONE
                validateForm() // Re-validate on return to idle
              }
              is CreateNapUiState.Saving -> {
                binding.buttonStartNap.isEnabled = false
                binding.buttonStartNap.text =
                    getString(R.string.create_nap_start_button) // Keep text, add progress
                binding.buttonStartNap.visibility = View.VISIBLE
                binding.progressSaving.visibility = View.VISIBLE
              }
              is CreateNapUiState.Success -> {
                findNavController()
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("nap_created", true)
                dismiss()
              }
              is CreateNapUiState.Error -> {
                binding.buttonStartNap.isEnabled = true
                binding.buttonStartNap.text = getString(R.string.create_nap_start_button)
                binding.buttonStartNap.visibility = View.VISIBLE
                binding.progressSaving.visibility = View.GONE
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              }
            }
          }
        }
        launch {
          viewModel.proposedDuration.collect { duration -> updateDurationDisplay(duration) }
        }
      }
    }
  }

  private fun updateTimestampDisplay() {
    binding.textStartTime.text =
        if (isNow(selectedTimestamp)) getString(R.string.create_nap_now)
        else formatTimestampForDisplay(requireContext(), selectedTimestamp)
    // Update ViewModel state for duration calculation
    viewModel.setStartTime(selectedTimestamp)
  }

  private fun updateEndTimeDisplay() {
    binding.textEndTime.text =
        selectedEndTimestamp?.let { formatTimestampForDisplay(requireContext(), it) }
            ?: getString(R.string.create_nap_end_not_set)
    // Update ViewModel state for duration calculation
    viewModel.setEndTime(selectedEndTimestamp)
  }

  private fun updateDurationDisplay(duration: String) {
    if (duration.isEmpty()) {
      binding.textProposedDuration.visibility = View.GONE
    } else {
      binding.textProposedDuration.text = getString(R.string.create_nap_duration, duration)
      binding.textProposedDuration.visibility = View.VISIBLE
    }
  }

  private fun updateEndTimeButtonLabel() {
    binding.buttonChangeEndTime.text =
        if (selectedEndTimestamp == null) getString(R.string.edit_nap_set_end_time)
        else getString(R.string.create_nap_change_time)
  }

  private fun isNow(iso: String): Boolean {
    return try {
      val instant = kotlinx.datetime.Instant.parse(iso)
      val now = Clock.System.now()
      kotlin.math.abs(instant.toEpochMilliseconds() - now.toEpochMilliseconds()) < 60_000
    } catch (_: Exception) {
      false
    }
  }

  private fun showStartDateTimePickers() {
    showDateTimePickers(selectedTimestamp) {
      selectedTimestamp = it
      updateTimestampDisplay()
      validateForm() // Validate after time change for real-time feedback
    }
  }

  private fun showEndDateTimePickers() {
    val initial = selectedEndTimestamp ?: selectedTimestamp
    showDateTimePickers(initial) {
      selectedEndTimestamp = it
      updateEndTimeDisplay()
      updateEndTimeButtonLabel()
      validateForm() // Validate after time change for real-time feedback
    }
  }

  private fun showDateTimePickers(initialIso: String, onSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    try {
      val instant = kotlinx.datetime.Instant.parse(initialIso)
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
        val iso = kotlinx.datetime.Instant.fromEpochMilliseconds(cal.timeInMillis).toString()
        onSelected(iso)
      }
      timePicker.show(parentFragmentManager, "time")
    }
    datePicker.show(parentFragmentManager, "date")
  }

  private fun startNap() {
    // Validate form before submitting
    if (!isFormValid()) {
      Snackbar.make(
              binding.root,
              getString(R.string.create_nap_end_must_be_after_start),
              Snackbar.LENGTH_LONG)
          .show()
      return
    }
    viewModel.createNap(selectedTimestamp, selectedEndTimestamp)
  }

  /** Check if form is valid (end_time after start_time if end_time is set). */
  private fun isFormValid(): Boolean {
    val endTime = selectedEndTimestamp ?: return true // No end time = valid
    return try {
      val startMs = kotlinx.datetime.Instant.parse(selectedTimestamp).toEpochMilliseconds()
      val endMs = kotlinx.datetime.Instant.parse(endTime).toEpochMilliseconds()
      endMs > startMs
    } catch (_: Exception) {
      false // Invalid timestamp format
    }
  }

  /** Validate form and update button state accordingly. */
  private fun validateForm() {
    val isValid = isFormValid()
    binding.buttonStartNap.isEnabled = isValid
    // Add visual feedback: you could add a red border to end_time field if invalid
    if (!isValid && selectedEndTimestamp != null) {
      binding.textEndTime.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
    } else {
      binding.textEndTime.setTextColor(
          binding.root.context.getColorStateList(android.R.color.darker_gray))
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

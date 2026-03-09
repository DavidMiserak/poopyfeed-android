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
    binding.buttonChangeTime.setOnClickListener { showStartDateTimePickers() }
    binding.buttonChangeEndTime.setOnClickListener { showEndDateTimePickers() }
    binding.buttonStartNap.setOnClickListener { startNap() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is CreateNapUiState.Idle -> {
              binding.buttonStartNap.isEnabled = true
              binding.buttonStartNap.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is CreateNapUiState.Saving -> {
              binding.buttonStartNap.isEnabled = false
              binding.buttonStartNap.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is CreateNapUiState.Success -> {
              findNavController().previousBackStackEntry?.savedStateHandle?.set("nap_created", true)
              dismiss()
            }
            is CreateNapUiState.Error -> {
              binding.buttonStartNap.isEnabled = true
              binding.buttonStartNap.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
          }
        }
      }
    }
  }

  private fun updateTimestampDisplay() {
    binding.textStartTime.text =
        if (isNow(selectedTimestamp)) getString(R.string.create_nap_now)
        else formatTimestampForDisplay(requireContext(), selectedTimestamp)
  }

  private fun updateEndTimeDisplay() {
    binding.textEndTime.text =
        selectedEndTimestamp?.let { formatTimestampForDisplay(requireContext(), it) }
            ?: getString(R.string.create_nap_end_not_set)
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
    }
  }

  private fun showEndDateTimePickers() {
    val initial = selectedEndTimestamp ?: selectedTimestamp
    showDateTimePickers(initial) {
      selectedEndTimestamp = it
      updateEndTimeDisplay()
      updateEndTimeButtonLabel()
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
    val endTime = selectedEndTimestamp
    if (endTime != null) {
      try {
        val startMs = kotlinx.datetime.Instant.parse(selectedTimestamp).toEpochMilliseconds()
        val endMs = kotlinx.datetime.Instant.parse(endTime).toEpochMilliseconds()
        if (endMs <= startMs) {
          Snackbar.make(
                  binding.root,
                  getString(R.string.create_nap_end_must_be_after_start),
                  Snackbar.LENGTH_LONG)
              .show()
          return
        }
      } catch (_: Exception) {}
    }
    viewModel.createNap(selectedTimestamp, selectedEndTimestamp)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

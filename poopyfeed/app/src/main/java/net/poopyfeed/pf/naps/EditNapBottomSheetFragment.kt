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
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.databinding.FragmentEditNapBottomSheetBinding
import net.poopyfeed.pf.util.formatTimestampForDisplay

/**
 * Bottom sheet for editing a nap. Prefills start time and optional end time. On success signals
 * nap_updated and dismisses.
 */
@AndroidEntryPoint
class EditNapBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentEditNapBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: EditNapViewModel by viewModels()
  private var selectedStartTime: String = ""
  private var selectedEndTime: String? = null
  private var formPrefilled: Boolean = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentEditNapBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.buttonChangeStartTime.setOnClickListener { showStartDateTimePickers() }
    binding.buttonChangeEndTime.setOnClickListener { showEndDateTimePickers() }
    binding.buttonSave.setOnClickListener { saveNap() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is EditNapUiState.Loading -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditNapUiState.Ready -> {
              if (!formPrefilled) {
                bindForm(state.nap)
                formPrefilled = true
              }
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is EditNapUiState.Error -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is EditNapUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditNapUiState.Success -> {
              findNavController().previousBackStackEntry?.savedStateHandle?.set("nap_updated", true)
              dismiss()
            }
            is EditNapUiState.SaveError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
          }
        }
      }
    }
  }

  private fun bindForm(nap: Nap) {
    selectedStartTime = nap.start_time
    selectedEndTime = nap.end_time
    updateStartTimeDisplay()
    updateEndTimeDisplay()
    updateEndTimeButtonLabel()
  }

  private fun updateStartTimeDisplay() {
    binding.textStartTime.text = formatTimestampForDisplay(requireContext(), selectedStartTime)
  }

  private fun updateEndTimeDisplay() {
    binding.textEndTime.text =
        selectedEndTime?.let { formatTimestampForDisplay(requireContext(), it) }
            ?: getString(R.string.nap_in_progress)
  }

  private fun updateEndTimeButtonLabel() {
    binding.buttonChangeEndTime.text =
        if (selectedEndTime == null) getString(R.string.edit_nap_set_end_time)
        else getString(R.string.create_nap_change_time)
  }

  private fun showStartDateTimePickers() {
    showDateTimePickers("start", selectedStartTime) {
      selectedStartTime = it
      updateStartTimeDisplay()
    }
  }

  private fun showEndDateTimePickers() {
    val initial = selectedEndTime ?: selectedStartTime
    showDateTimePickers("end", initial) {
      selectedEndTime = it
      updateEndTimeDisplay()
      updateEndTimeButtonLabel()
    }
  }

  private fun showDateTimePickers(
      tagPrefix: String,
      initialIso: String,
      onSelected: (String) -> Unit,
  ) {
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
      timePicker.show(parentFragmentManager, "${tagPrefix}_time")
    }
    datePicker.show(parentFragmentManager, "${tagPrefix}_date")
  }

  private fun saveNap() {
    viewModel.saveNap(selectedStartTime, selectedEndTime)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

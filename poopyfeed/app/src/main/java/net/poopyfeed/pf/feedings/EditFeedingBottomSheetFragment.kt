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
import java.util.Locale
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.databinding.FragmentCreateFeedingBottomSheetBinding
import net.poopyfeed.pf.util.formatTimestampForDisplay

/**
 * Bottom sheet for editing an existing feeding. Reuses the create feeding form layout; prefills
 * type, amount (bottle), and timestamp. On success signals feeding_updated and dismisses.
 */
@AndroidEntryPoint
class EditFeedingBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateFeedingBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: EditFeedingViewModel by viewModels()
  private var selectedTimestamp: String = ""
  private var formPrefilled: Boolean = false

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
    binding.textSheetTitle.setText(R.string.edit_feeding_title)
    binding.radioFeedingType.setOnCheckedChangeListener { _, checkedId ->
      val isBottle = checkedId == R.id.radio_bottle
      binding.layoutAmount.visibility = if (isBottle) View.VISIBLE else View.GONE
      binding.layoutMinutes.visibility = if (isBottle) View.GONE else View.VISIBLE
      binding.labelSide.visibility = if (isBottle) View.GONE else View.VISIBLE
      binding.radioSide.visibility = if (isBottle) View.GONE else View.VISIBLE
    }
    binding.buttonChangeTime.setOnClickListener { showDateTimePickers() }
    binding.buttonSave.setOnClickListener { saveFeeding() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is EditFeedingUiState.Loading -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditFeedingUiState.Ready -> {
              if (!formPrefilled) {
                bindForm(state.feeding)
                formPrefilled = true
              }
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
            }
            is EditFeedingUiState.Error -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is EditFeedingUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditFeedingUiState.Success -> {
              findNavController()
                  .previousBackStackEntry
                  ?.savedStateHandle
                  ?.set("feeding_updated", true)
              dismiss()
            }
            is EditFeedingUiState.SaveError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is EditFeedingUiState.ValidationError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              state.typeError?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
              binding.layoutAmount.error = state.amountError
              binding.layoutMinutes.error = state.minutesError
              state.sideError?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
            }
          }
        }
      }
    }
  }

  private fun bindForm(feeding: Feeding) {
    val isBottle = feeding.feeding_type.equals("bottle", ignoreCase = true)
    binding.radioBottle.isChecked = isBottle
    binding.radioBreast.isChecked = !isBottle
    binding.layoutAmount.visibility = if (isBottle) View.VISIBLE else View.GONE
    binding.layoutMinutes.visibility = if (isBottle) View.GONE else View.VISIBLE
    binding.labelSide.visibility = if (isBottle) View.GONE else View.VISIBLE
    binding.radioSide.visibility = if (isBottle) View.GONE else View.VISIBLE
    if (isBottle) {
      if (feeding.amount_oz != null) {
        binding.inputAmount.setText(String.format(Locale.getDefault(), "%.1f", feeding.amount_oz))
      } else {
        binding.inputAmount.text?.clear()
      }
      binding.inputMinutes.text?.clear()
    } else {
      binding.inputAmount.text?.clear()
      if (feeding.duration_minutes != null) {
        binding.inputMinutes.setText(
            String.format(Locale.getDefault(), "%d", feeding.duration_minutes))
      } else {
        binding.inputMinutes.text?.clear()
      }
      when (feeding.side?.lowercase()) {
        "left" -> binding.radioSideLeft.isChecked = true
        "right" -> binding.radioSideRight.isChecked = true
        "both" -> binding.radioSideBoth.isChecked = true
        else -> binding.radioSide.clearCheck()
      }
    }
    selectedTimestamp = feeding.timestamp
    updateTimestampDisplay()
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
    val minutesStr = binding.inputMinutes.text?.toString()?.trim()
    val minutes = minutesStr?.toIntOrNull()
    val side =
        when (binding.radioSide.checkedRadioButtonId) {
          R.id.radio_side_left -> "left"
          R.id.radio_side_right -> "right"
          R.id.radio_side_both -> "both"
          else -> ""
        }
    viewModel.saveFeeding(type, amount, minutes, side, selectedTimestamp)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

package net.poopyfeed.pf.children

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentEditChildBottomSheetBinding

/**
 * Bottom sheet for editing a child: name, DOB, gender, and (for owner/co-parent) feeding reminder
 * interval. Saves via PATCH; on success dismisses and sets savedStateHandle so the detail screen
 * can refresh.
 */
@AndroidEntryPoint
class EditChildBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentEditChildBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: EditChildViewModel by viewModels()
  private var selectedDate: String? = null

  /** Spinner options: (display label, value in hours or null for Off). Built in onViewCreated. */
  private lateinit var feedingReminderOptions: List<Pair<String, Int?>>

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentEditChildBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    feedingReminderOptions =
        listOf(
            Pair(getString(R.string.feeding_reminder_off), null),
            Pair(getString(R.string.feeding_reminder_2h), 2),
            Pair(getString(R.string.feeding_reminder_3h), 3),
            Pair(getString(R.string.feeding_reminder_4h), 4),
            Pair(getString(R.string.feeding_reminder_6h), 6),
        )
    setupFeedingReminderSpinner()
    binding.inputDob.setOnClickListener { showDatePicker() }
    binding.buttonSave.setOnClickListener { saveChild() }
    binding.buttonDelete.setOnClickListener { showDeleteConfirmationDialog() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is EditChildUiState.Loading -> {
              binding.buttonSave.isEnabled = false
              binding.progressSaving.visibility = View.GONE
            }
            is EditChildUiState.Ready -> bindForm(state)
            is EditChildUiState.Error -> {
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              dismiss()
            }
            is EditChildUiState.Saving -> {
              binding.buttonSave.isEnabled = false
              binding.buttonSave.visibility = View.GONE
              binding.progressSaving.visibility = View.VISIBLE
            }
            is EditChildUiState.Success -> {
              findNavController()
                  .previousBackStackEntry
                  ?.savedStateHandle
                  ?.set("child_updated", true)
              Snackbar.make(binding.root, R.string.edit_child_success, Snackbar.LENGTH_SHORT).show()
              dismiss()
            }
            is EditChildUiState.SaveError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
            is EditChildUiState.ValidationError -> {
              binding.buttonSave.isEnabled = true
              binding.buttonSave.visibility = View.VISIBLE
              binding.progressSaving.visibility = View.GONE
              binding.layoutName.error = state.nameError
              binding.layoutDob.error = state.dobError
            }
          }
        }
      }
    }
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.deleteSuccess.collect {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("child_deleted", true)
            dismiss()
          }
        }
        launch {
          viewModel.deleteError.collect { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  private fun setupFeedingReminderSpinner() {
    val labels = feedingReminderOptions.map { it.first }
    val adapter =
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels).apply {
          setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    binding.spinnerFeedingReminder.adapter = adapter
  }

  private fun bindForm(state: EditChildUiState.Ready) {
    val child = state.child
    binding.inputName.setText(child.name)
    binding.inputDob.setText(child.date_of_birth)
    selectedDate = child.date_of_birth
    binding.radioGender.check(
        when (child.gender) {
          "M" -> R.id.radio_boy
          "F" -> R.id.radio_girl
          else -> R.id.radio_boy
        })
    val reminderIndex =
        feedingReminderOptions
            .indexOfFirst { it.second == child.feeding_reminder_interval }
            .let { if (it < 0) 0 else it }
    binding.spinnerFeedingReminder.setSelection(reminderIndex)

    val showReminder = state.canEditReminder
    binding.labelFeedingReminder.visibility = if (showReminder) View.VISIBLE else View.GONE
    binding.spinnerFeedingReminder.visibility = if (showReminder) View.VISIBLE else View.GONE

    val isOwner = child.user_role == "owner"
    binding.buttonDelete.visibility = if (isOwner) View.VISIBLE else View.GONE

    binding.buttonSave.isEnabled = true
    binding.buttonSave.visibility = View.VISIBLE
    binding.progressSaving.visibility = View.GONE
    binding.layoutName.error = null
    binding.layoutDob.error = null
  }

  private fun showDatePicker() {
    val (year, month, day) =
        try {
          val parts = (selectedDate ?: binding.inputDob.text.toString()).split("-")
          if (parts.size == 3) {
            Triple(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
          } else {
            val cal = Calendar.getInstance()
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
          }
        } catch (_: Exception) {
          val cal = Calendar.getInstance()
          Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }

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

  private fun showDeleteConfirmationDialog() {
    val state = viewModel.uiState.value
    if (state !is EditChildUiState.Ready) return
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.child_detail_delete_title)
        .setMessage(getString(R.string.child_detail_delete_message, state.child.name))
        .setPositiveButton(R.string.child_detail_delete_confirm) { _, _ -> viewModel.deleteChild() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun saveChild() {
    val name = binding.inputName.text.toString().trim()
    val dob = selectedDate ?: binding.inputDob.text.toString()
    val gender =
        when (binding.radioGender.checkedRadioButtonId) {
          R.id.radio_boy -> "M"
          R.id.radio_girl -> "F"
          else -> "M"
        }
    val reminderHours =
        if (binding.spinnerFeedingReminder.visibility == View.VISIBLE) {
          feedingReminderOptions
              .getOrNull(binding.spinnerFeedingReminder.selectedItemPosition)
              ?.second
        } else {
          null
        }
    viewModel.save(name, dob, gender, reminderHours)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

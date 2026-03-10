package net.poopyfeed.pf.children

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentEditChildBottomSheetBinding
import net.poopyfeed.pf.util.logScreenView

/**
 * Full-screen fragment for editing a child: name, DOB, gender, custom bottle amounts, feeding
 * reminder interval, and notification preferences. Saves via PATCH; on success pops back and sets
 * savedStateHandle so the detail screen can refresh.
 */
@AndroidEntryPoint
class EditChildFragment : Fragment() {

  private var _binding: FragmentEditChildBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: EditChildViewModel by viewModels()
  private var selectedDate: String? = null

  /** Spinner options: (display label, value in hours or null for Off). Built in onViewCreated. */
  private lateinit var feedingReminderOptions: List<Pair<String, Int?>>

  /** Suppress checkbox listener callbacks when programmatically setting checked state. */
  private var suppressPrefListeners = false

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

    logScreenView(viewModel.analyticsTracker, "EditChild")

    binding.dragHandle.visibility = View.GONE

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
    binding.buttonRestoreDefaults.setOnClickListener { restoreBottleDefaults() }
    setupNotificationPrefListeners()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.uiState.collect { state ->
            when (state) {
              is EditChildUiState.Loading -> {
                binding.buttonSave.isEnabled = false
                binding.progressSaving.visibility = View.GONE
              }
              is EditChildUiState.Ready -> bindForm(state)
              is EditChildUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack()
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
                Snackbar.make(binding.root, R.string.edit_child_success, Snackbar.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
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
                if (state.bottleError != null) {
                  binding.textBottleError.text = state.bottleError
                  binding.textBottleError.visibility = View.VISIBLE
                } else {
                  binding.textBottleError.visibility = View.GONE
                }
              }
            }
          }
        }
        launch {
          viewModel.notificationPrefState.collect { state -> bindNotificationPrefState(state) }
        }
        launch {
          viewModel.preferenceSaving.collect { saving ->
            binding.textPrefsSaving.visibility = if (saving) View.VISIBLE else View.GONE
            binding.checkboxNotifyFeedings.isEnabled = !saving
            binding.checkboxNotifyDiapers.isEnabled = !saving
            binding.checkboxNotifyNaps.isEnabled = !saving
          }
        }
        launch {
          viewModel.deleteSuccess.collect {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("child_deleted", true)
            findNavController().popBackStack()
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

  private fun setupNotificationPrefListeners() {
    binding.checkboxNotifyFeedings.setOnCheckedChangeListener { _, isChecked ->
      if (!suppressPrefListeners) viewModel.toggleNotificationPref("notify_feedings", isChecked)
    }
    binding.checkboxNotifyDiapers.setOnCheckedChangeListener { _, isChecked ->
      if (!suppressPrefListeners) viewModel.toggleNotificationPref("notify_diapers", isChecked)
    }
    binding.checkboxNotifyNaps.setOnCheckedChangeListener { _, isChecked ->
      if (!suppressPrefListeners) viewModel.toggleNotificationPref("notify_naps", isChecked)
    }
  }

  private fun bindNotificationPrefState(state: NotificationPrefState?) {
    when (state) {
      null -> {
        binding.cardNotificationPrefs.visibility = View.GONE
      }
      is NotificationPrefState.Loading -> {
        binding.cardNotificationPrefs.visibility = View.VISIBLE
        binding.layoutPrefsLoading.visibility = View.VISIBLE
        binding.layoutPrefsCheckboxes.visibility = View.GONE
        binding.textPrefsError.visibility = View.GONE
      }
      is NotificationPrefState.Loaded -> {
        binding.cardNotificationPrefs.visibility = View.VISIBLE
        binding.layoutPrefsLoading.visibility = View.GONE
        binding.textPrefsError.visibility = View.GONE
        binding.layoutPrefsCheckboxes.visibility = View.VISIBLE
        suppressPrefListeners = true
        binding.checkboxNotifyFeedings.isChecked = state.pref.notifyFeedings
        binding.checkboxNotifyDiapers.isChecked = state.pref.notifyDiapers
        binding.checkboxNotifyNaps.isChecked = state.pref.notifyNaps
        suppressPrefListeners = false
      }
      is NotificationPrefState.Error -> {
        binding.cardNotificationPrefs.visibility = View.VISIBLE
        binding.layoutPrefsLoading.visibility = View.GONE
        binding.layoutPrefsCheckboxes.visibility = View.GONE
        binding.textPrefsError.visibility = View.VISIBLE
      }
    }
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
          "O" -> R.id.radio_other
          else -> R.id.radio_boy
        })
    val reminderIndex =
        feedingReminderOptions
            .indexOfFirst { it.second == child.feeding_reminder_interval }
            .let { if (it < 0) 0 else it }
    binding.spinnerFeedingReminder.setSelection(reminderIndex)

    child.custom_bottle_low_oz?.let { binding.inputBottleLow.setText(it) }
    child.custom_bottle_mid_oz?.let { binding.inputBottleMid.setText(it) }
    child.custom_bottle_high_oz?.let { binding.inputBottleHigh.setText(it) }

    val showReminder = state.canEditReminder
    binding.cardFeedingReminder.isVisible = showReminder

    val isOwner = child.user_role == "owner"
    binding.cardDangerZone.visibility = if (isOwner) View.VISIBLE else View.GONE

    binding.buttonSave.isEnabled = true
    binding.buttonSave.visibility = View.VISIBLE
    binding.progressSaving.visibility = View.GONE
    binding.layoutName.error = null
    binding.layoutDob.error = null
    binding.textBottleError.visibility = View.GONE
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

  private fun restoreBottleDefaults() {
    binding.inputBottleLow.text?.clear()
    binding.inputBottleMid.text?.clear()
    binding.inputBottleHigh.text?.clear()
    binding.textBottleError.visibility = View.GONE
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
          R.id.radio_other -> "O"
          else -> "M"
        }
    val reminderHours =
        if (binding.cardFeedingReminder.isVisible) {
          feedingReminderOptions
              .getOrNull(binding.spinnerFeedingReminder.selectedItemPosition)
              ?.second
        } else {
          null
        }
    val bottleLow = binding.inputBottleLow.text.toString()
    val bottleMid = binding.inputBottleMid.text.toString()
    val bottleHigh = binding.inputBottleHigh.text.toString()
    viewModel.save(name, dob, gender, reminderHours, bottleLow, bottleMid, bottleHigh)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

package net.poopyfeed.pf.children

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
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentChildDetailQuickLogSheetBinding

/**
 * Bottom sheet shown when the user taps the FAB on the child detail page. Offers quick logging:
 * - Feeding: three bottle options (2 oz, 4 oz, 6 oz) that log immediately with time = now; backend
 *   sets the amounts.
 * - Diaper: three options (wet, dirty, both) that log immediately with time = now.
 * - Nap: starts a nap with start time = now (in progress), one tap.
 */
@AndroidEntryPoint
class ChildDetailQuickLogBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentChildDetailQuickLogSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ChildDetailQuickLogViewModel by viewModels()

  private val childId: Int
    get() = requireArguments().getInt(KEY_CHILD_ID)

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentChildDetailQuickLogSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.bottleAmounts.collect { amounts ->
          binding.buttonBottleLow.text = formatBottleOz(amounts.lowOz)
          binding.buttonBottleMid.text = formatBottleOz(amounts.midOz)
          binding.buttonBottleHigh.text = formatBottleOz(amounts.highOz)
        }
      }
    }

    binding.buttonBottleLow.setOnClickListener {
      viewModel.bottleAmounts.value.let { viewModel.createBottleNow(it.lowOz) }
    }
    binding.buttonBottleMid.setOnClickListener {
      viewModel.bottleAmounts.value.let { viewModel.createBottleNow(it.midOz) }
    }
    binding.buttonBottleHigh.setOnClickListener {
      viewModel.bottleAmounts.value.let { viewModel.createBottleNow(it.highOz) }
    }

    binding.buttonDiaperWet.setOnClickListener { viewModel.createDiaperNow("wet") }
    binding.buttonDiaperDirty.setOnClickListener { viewModel.createDiaperNow("dirty") }
    binding.buttonDiaperBoth.setOnClickListener { viewModel.createDiaperNow("both") }

    binding.buttonLogNap.setOnClickListener { viewModel.createNapNow() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.diaperState.collect { state ->
            when (state) {
              is QuickLogDiaperUiState.Idle -> setDiaperButtonsEnabled(true)
              is QuickLogDiaperUiState.Saving -> setDiaperButtonsEnabled(false)
              is QuickLogDiaperUiState.Success -> {
                findNavController()
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("diaper_created", true)
                dismiss()
              }
              is QuickLogDiaperUiState.Error -> {
                setDiaperButtonsEnabled(true)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              }
            }
          }
        }
        launch {
          viewModel.feedingState.collect { state ->
            when (state) {
              is QuickLogFeedingUiState.Idle -> setFeedingButtonsEnabled(true)
              is QuickLogFeedingUiState.Saving -> setFeedingButtonsEnabled(false)
              is QuickLogFeedingUiState.Success -> {
                findNavController()
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("feeding_created", true)
                dismiss()
              }
              is QuickLogFeedingUiState.Error -> {
                setFeedingButtonsEnabled(true)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              }
            }
          }
        }
        launch {
          viewModel.napState.collect { state ->
            when (state) {
              is QuickLogNapUiState.Idle -> setNapButtonEnabled(true)
              is QuickLogNapUiState.Saving -> setNapButtonEnabled(false)
              is QuickLogNapUiState.Success -> {
                findNavController()
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("nap_created", true)
                dismiss()
              }
              is QuickLogNapUiState.Error -> {
                setNapButtonEnabled(true)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
              }
            }
          }
        }
      }
    }
  }

  private fun setDiaperButtonsEnabled(enabled: Boolean) {
    binding.buttonDiaperWet.isEnabled = enabled
    binding.buttonDiaperDirty.isEnabled = enabled
    binding.buttonDiaperBoth.isEnabled = enabled
  }

  private fun setFeedingButtonsEnabled(enabled: Boolean) {
    binding.buttonBottleLow.isEnabled = enabled
    binding.buttonBottleMid.isEnabled = enabled
    binding.buttonBottleHigh.isEnabled = enabled
  }

  private fun setNapButtonEnabled(enabled: Boolean) {
    binding.buttonLogNap.isEnabled = enabled
  }

  private fun formatBottleOz(oz: Double): String {
    val whole = oz.toLong()
    return if (oz == whole.toDouble()) getString(R.string.quick_log_bottle_oz_int, whole)
    else getString(R.string.quick_log_bottle_oz_float, oz)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    private const val KEY_CHILD_ID = "childId"
  }
}

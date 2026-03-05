package net.poopyfeed.pf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.databinding.FragmentChildDetailBinding

/**
 * Displays details of a single child including name, age, gender, and last activities. Shows delete
 * and edit buttons for the owner. Allows deletion with confirmation dialog.
 */
@AndroidEntryPoint
class ChildDetailFragment : Fragment() {

  private var _binding: FragmentChildDetailBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ChildDetailViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentChildDetailBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Setup delete button click listener
    binding.buttonDelete.setOnClickListener { showDeleteConfirmationDialog() }

    // Collect UI state
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is ChildDetailUiState.Loading -> {
              // optional: show loading UI
            }
            is ChildDetailUiState.Ready -> {
              // Update toolbar title with child name
              (activity as? AppCompatActivity)?.supportActionBar?.title = state.child.name
              binding.textChildName.text = state.child.name
              binding.textAgeGender.text = state.ageFormatted

              // Update activity labels with formatted times
              binding.labelFeeding.text =
                  getString(R.string.child_detail_last_feeding, state.lastFeedingFormatted)
              binding.labelDiaper.text =
                  getString(R.string.child_detail_last_diaper, state.lastDiaperFormatted)
              binding.labelNap.text =
                  getString(R.string.child_detail_last_nap, state.lastNapFormatted)

              // Show/hide role badge for non-owners
              binding.chipRole.visibility = if (state.isOwner) View.GONE else View.VISIBLE
              if (!state.isOwner) {
                binding.chipRole.text =
                    state.child.user_role.replaceFirstChar { it.uppercaseChar() }
              }

              // Show delete and edit buttons for owner
              binding.buttonDelete.visibility = if (state.isOwner) View.VISIBLE else View.GONE
              binding.buttonEdit.visibility = if (state.isOwner) View.VISIBLE else View.GONE
            }
            is ChildDetailUiState.Error -> {
              Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
          }
        }
      }
    }

    // Collect navigate back event
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.navigateBack.collect { findNavController().popBackStack() }
      }
    }

    // Collect delete error events
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.deleteError.collect { message ->
          Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
      }
    }
  }

  private fun showDeleteConfirmationDialog() {
    val state = viewModel.uiState.value
    if (state !is ChildDetailUiState.Ready) return

    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.child_detail_delete_title)
        .setMessage(getString(R.string.child_detail_delete_message, state.child.name))
        .setPositiveButton(R.string.child_detail_delete_confirm) { _, _ -> viewModel.deleteChild() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

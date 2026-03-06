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

    binding.buttonDelete.setOnClickListener { showDeleteConfirmationDialog() }

    binding.buttonFeedings.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_feedingsList, bundle)
    }
    binding.buttonDiapers.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_diapersList, bundle)
    }
    binding.buttonNaps.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_napsList, bundle)
    }

    collectFlows()
  }

  private fun collectFlows() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { updateUiState(it) } }
        launch { viewModel.navigateBack.collect { findNavController().popBackStack() } }
        launch {
          viewModel.deleteError.collect { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  private fun updateUiState(state: ChildDetailUiState) {
    when (state) {
      is ChildDetailUiState.Loading -> Unit
      is ChildDetailUiState.Ready -> bindReadyState(state)
      is ChildDetailUiState.Error ->
          Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
    }
  }

  private fun bindReadyState(state: ChildDetailUiState.Ready) {
    (activity as? AppCompatActivity)?.supportActionBar?.title = state.child.name
    binding.textChildName.text = state.child.name
    binding.textAgeGender.text = state.ageFormatted
    binding.labelFeeding.text =
        getString(R.string.child_detail_last_feeding, state.lastFeedingFormatted)
    binding.labelDiaper.text =
        getString(R.string.child_detail_last_diaper, state.lastDiaperFormatted)
    binding.labelNap.text = getString(R.string.child_detail_last_nap, state.lastNapFormatted)
    binding.chipRole.visibility = if (state.isOwner) View.GONE else View.VISIBLE
    if (!state.isOwner) {
      binding.chipRole.text = state.child.user_role.replaceFirstChar { it.uppercaseChar() }
    }
    binding.buttonDelete.visibility = if (state.isOwner) View.VISIBLE else View.GONE
    binding.buttonEdit.visibility = if (state.isOwner) View.VISIBLE else View.GONE
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

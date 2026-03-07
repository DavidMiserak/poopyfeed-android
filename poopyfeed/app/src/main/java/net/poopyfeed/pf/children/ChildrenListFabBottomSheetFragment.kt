package net.poopyfeed.pf.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentChildrenListFabSheetBinding

/**
 * Bottom sheet shown when the user taps the FAB on the Children list. Offers "Add Child" and "Enter
 * invite code". On successful invite accept, sets fragment result with childId so
 * ChildrenListFragment can navigate to child detail.
 */
@AndroidEntryPoint
class ChildrenListFabBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentChildrenListFabSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ChildrenListFabViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentChildrenListFabSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.buttonAddChild.setOnClickListener {
      dismiss()
      findNavController()
          .navigate(
              R.id.createChildBottomSheet,
              null,
              androidx.navigation.NavOptions.Builder()
                  .setPopUpTo(R.id.childrenListFabSheet, true)
                  .build())
    }

    binding.buttonEnterInviteCode.setOnClickListener { showEnterCodeDialog() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.navigateToChildId.collect { childId ->
            setFragmentResult(
                ACCEPT_INVITE_RESULT_KEY, Bundle().apply { putInt(KEY_CHILD_ID, childId) })
            dismiss()
          }
        }
        launch {
          viewModel.errorMessage.collect { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  private fun showEnterCodeDialog() {
    val edit =
        android.widget.EditText(requireContext()).apply {
          hint = getString(R.string.pending_invites_token_hint)
          setPadding(
              resources.getDimensionPixelSize(android.R.dimen.app_icon_size),
              resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 2,
              resources.getDimensionPixelSize(android.R.dimen.app_icon_size),
              resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 2,
          )
        }
    AlertDialog.Builder(requireContext())
        .setTitle(R.string.pending_invites_enter_token_title)
        .setView(edit)
        .setPositiveButton(R.string.pending_invite_accept) { _, _ ->
          val input = edit.text?.toString()?.trim() ?: ""
          val token = extractTokenFromInput(input)
          viewModel.acceptByToken(token)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  private fun extractTokenFromInput(input: String): String {
    if (input.isEmpty()) return input
    val acceptInvite = "accept-invite/"
    val idx = input.indexOf(acceptInvite)
    return if (idx >= 0) {
      val start = idx + acceptInvite.length
      val rest = input.substring(start)
      rest.takeWhile { it != '/' && it != '?' && !it.isWhitespace() }
    } else {
      input
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    const val ACCEPT_INVITE_RESULT_KEY = "accept_invite_child_id"
    const val KEY_CHILD_ID = "childId"
  }
}

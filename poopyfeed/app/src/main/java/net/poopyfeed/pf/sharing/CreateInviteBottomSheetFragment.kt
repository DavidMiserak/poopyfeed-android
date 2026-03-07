package net.poopyfeed.pf.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentCreateInviteBottomSheetBinding

/**
 * Bottom sheet for creating a token-based invite (role only). Backend returns a shareable link. On
 * success shows the link for copy/share (matches web "Tap to Copy Link"); user taps Done to
 * dismiss.
 */
@AndroidEntryPoint
class CreateInviteBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentCreateInviteBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: CreateInviteViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCreateInviteBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.chipCoParent.setOnClickListener { viewModel.setRole("co-parent") }
    binding.chipCaregiver.setOnClickListener { viewModel.setRole("caregiver") }
    binding.buttonSend.setOnClickListener { viewModel.submit() }
    binding.buttonCopyCode.setOnClickListener {
      val code = binding.textInviteCode.text?.toString() ?: return@setOnClickListener
      (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
          .setPrimaryClip(ClipData.newPlainText(getString(R.string.invite_code_label), code))
      Snackbar.make(
              binding.root, getString(R.string.create_invite_code_copied), Snackbar.LENGTH_SHORT)
          .show()
    }
    binding.buttonDone.setOnClickListener { dismiss() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is CreateInviteUiState.Ready -> {
              binding.panelForm.visibility = View.VISIBLE
              binding.panelInviteCreated.visibility = View.GONE
              binding.textRoleError.text = state.roleError
              binding.textRoleError.visibility =
                  if (state.roleError != null) View.VISIBLE else View.GONE
              binding.chipCoParent.isChecked = state.selectedRole == "co-parent"
              binding.chipCaregiver.isChecked = state.selectedRole == "caregiver"
              binding.buttonSend.isEnabled = true
            }
            is CreateInviteUiState.Submitting -> {
              binding.buttonSend.isEnabled = false
            }
            is CreateInviteUiState.InviteCreated -> {
              binding.panelForm.visibility = View.GONE
              binding.panelInviteCreated.visibility = View.VISIBLE
              binding.textInviteCode.text = state.inviteCode
            }
          }
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.errorMessage.collect { message ->
          Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

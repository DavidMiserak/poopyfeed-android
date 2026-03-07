package net.poopyfeed.pf.sharing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentPendingInvitesBinding

/**
 * Lists pending share invites. User can accept an invite; on success they are taken to that child's
 * detail screen.
 */
@AndroidEntryPoint
class PendingInvitesFragment : Fragment() {

  private var _binding: FragmentPendingInvitesBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: PendingInvitesViewModel by viewModels()
  private lateinit var adapter: PendingInviteAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentPendingInvitesBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    adapter = PendingInviteAdapter { _, _ ->
      // Accept is by token only (backend has no pending list); use "I have an invite link" flow
    }
    binding.recyclerPendingInvites.adapter = adapter
    binding.recyclerPendingInvites.layoutManager = LinearLayoutManager(requireContext())

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }
    binding.layoutEmptyState.findViewById<View>(R.id.button_accept_by_token)?.setOnClickListener {
      showAcceptByTokenDialog()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is PendingInvitesUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerPendingInvites.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.swipeRefresh.isRefreshing = true
            }
            is PendingInvitesUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerPendingInvites.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              adapter.submitList(state.invites)
            }
            is PendingInvitesUiState.Empty -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerPendingInvites.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
            is PendingInvitesUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerPendingInvites.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.VISIBLE
              binding.layoutErrorState
                  .findViewById<android.widget.TextView>(R.id.text_error_message)
                  .text = state.message
            }
          }
          binding.swipeRefresh.isRefreshing = state is PendingInvitesUiState.Loading
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.navigateToChild.collect { childId ->
          val bundle = Bundle().apply { putInt("childId", childId) }
          findNavController().navigate(R.id.action_pendingInvitesFragment_to_childDetail, bundle)
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

  private fun showAcceptByTokenDialog() {
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

  /** Extract invite token from pasted link (e.g. …/accept-invite/TOKEN/) or return input as-is. */
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
}

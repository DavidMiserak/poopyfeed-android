package net.poopyfeed.pf.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import net.poopyfeed.pf.data.models.ChildInvite
import net.poopyfeed.pf.databinding.FragmentSharingBinding

/**
 * Manage sharing for a child: invite links (copy/pause/delete) and people with access; create
 * invite via FAB.
 */
@AndroidEntryPoint
class SharingFragment : Fragment(), SharingInviteCallbacks {

  private var _binding: FragmentSharingBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: SharingViewModel by viewModels()
  private lateinit var adapter: SharingAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSharingBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    adapter = SharingAdapter(this)
    binding.recyclerSharing.adapter = adapter
    binding.recyclerSharing.layoutManager = LinearLayoutManager(requireContext())

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }
    binding.fabInvite.setOnClickListener { openCreateInvite() }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is SharingUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerSharing.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.fabInvite.visibility = View.GONE
              binding.swipeRefresh.isRefreshing = true
            }
            is SharingUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.fabInvite.visibility = View.VISIBLE
              val showList = state.items.size > 2
              binding.recyclerSharing.visibility = if (showList) View.VISIBLE else View.GONE
              binding.layoutEmptyState.visibility = if (showList) View.GONE else View.VISIBLE
              adapter.submitList(state.items)
            }
            is SharingUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerSharing.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.VISIBLE
              binding.fabInvite.visibility = View.GONE
              binding.layoutErrorState
                  .findViewById<android.widget.TextView>(R.id.text_error_message)
                  .text = state.message
            }
          }
          binding.swipeRefresh.isRefreshing =
              viewModel.isRefreshing.value || state is SharingUiState.Loading
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.isRefreshing.collect { refreshing ->
          binding.swipeRefresh.isRefreshing =
              refreshing || viewModel.uiState.value is SharingUiState.Loading
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

  override fun onCopyLink(invite: ChildInvite) {
    (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText(getString(R.string.invite_code_label), invite.token))
    Snackbar.make(binding.root, getString(R.string.invite_code_copied), Snackbar.LENGTH_SHORT)
        .show()
  }

  override fun onToggleInvite(invite: ChildInvite) {
    viewModel.toggleInvite(invite)
  }

  override fun onDeleteInvite(invite: ChildInvite) {
    viewModel.deleteInvite(invite)
  }

  override fun onResume() {
    super.onResume()
    // Refresh when returning from CreateInvite bottom sheet (invite may have been created)
    if (viewModel.uiState.value is SharingUiState.Ready) {
      viewModel.refresh()
    }
  }

  private fun openCreateInvite() {
    findNavController()
        .navigate(
            R.id.action_sharingFragment_to_createInviteBottomSheet,
            Bundle().apply { putInt("childId", viewModel.childId) })
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

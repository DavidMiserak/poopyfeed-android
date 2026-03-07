package net.poopyfeed.pf.sharing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.ItemPendingInviteBinding

/**
 * RecyclerView adapter for pending share invites. Displays child name (or fallback), role, and
 * Accept button.
 */
class PendingInviteAdapter(
    private val onAcceptClick: (inviteId: Int, childId: Int) -> Unit,
) : ListAdapter<ShareInviteWithChildName, PendingInviteAdapter.ViewHolder>(DiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding =
        ItemPendingInviteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding, onAcceptClick)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class ViewHolder(
      private val binding: ItemPendingInviteBinding,
      private val onAcceptClick: (inviteId: Int, childId: Int) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ShareInviteWithChildName) {
      val invite = item.invite
      binding.textTitle.text =
          item.childName?.let { name ->
            binding.root.context.getString(R.string.pending_invite_for_child, name)
          } ?: binding.root.context.getString(R.string.pending_invite_for_role, invite.role)
      binding.textRole.text = formatRole(invite.role)
      binding.buttonAccept.setOnClickListener { onAcceptClick(invite.id, invite.child) }
    }

    private fun formatRole(role: String): String =
        when (role) {
          "co-parent" -> binding.root.context.getString(R.string.sharing_role_co_parent)
          "caregiver" -> binding.root.context.getString(R.string.sharing_role_caregiver)
          else -> role
        }
  }

  private object DiffCallback : DiffUtil.ItemCallback<ShareInviteWithChildName>() {
    override fun areItemsTheSame(old: ShareInviteWithChildName, new: ShareInviteWithChildName) =
        old.invite.id == new.invite.id

    override fun areContentsTheSame(old: ShareInviteWithChildName, new: ShareInviteWithChildName) =
        old == new
  }
}

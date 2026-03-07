package net.poopyfeed.pf.sharing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.ChildInvite
import net.poopyfeed.pf.data.models.ChildShare
import net.poopyfeed.pf.databinding.ItemInviteLinkBinding
import net.poopyfeed.pf.databinding.ItemSectionHeaderBinding
import net.poopyfeed.pf.databinding.ItemSharingBinding

/** Callbacks for invite link row actions. */
interface SharingInviteCallbacks {
  fun onCopyLink(invite: ChildInvite)

  fun onToggleInvite(invite: ChildInvite)

  fun onDeleteInvite(invite: ChildInvite)
}

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_INVITE = 1
private const val VIEW_TYPE_SHARE = 2

/**
 * RecyclerView adapter for the sharing screen: section headers (Invite links, People with access),
 * invite link rows (copy/pause/delete), and share rows (email, role).
 */
class SharingAdapter(
    private val inviteCallbacks: SharingInviteCallbacks,
) : ListAdapter<SharingListItem, RecyclerView.ViewHolder>(DiffCallback) {

  override fun getItemViewType(position: Int): Int =
      when (getItem(position)) {
        is SharingListItem.InviteLinkHeader,
        is SharingListItem.PeopleHeader -> VIEW_TYPE_HEADER
        is SharingListItem.InviteRow -> VIEW_TYPE_INVITE
        is SharingListItem.ShareRow -> VIEW_TYPE_SHARE
      }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      when (viewType) {
        VIEW_TYPE_HEADER -> {
          val binding =
              ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
          SectionHeaderViewHolder(binding)
        }
        VIEW_TYPE_INVITE -> {
          val binding =
              ItemInviteLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
          InviteRowViewHolder(binding, inviteCallbacks)
        }
        VIEW_TYPE_SHARE -> {
          val binding =
              ItemSharingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
          ShareRowViewHolder(binding)
        }
        else -> error("Unknown viewType $viewType")
      }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (val item = getItem(position)) {
      is SharingListItem.InviteLinkHeader -> (holder as SectionHeaderViewHolder).bind(item.title)
      is SharingListItem.PeopleHeader -> (holder as SectionHeaderViewHolder).bind(item.title)
      is SharingListItem.InviteRow -> (holder as InviteRowViewHolder).bind(item.invite)
      is SharingListItem.ShareRow -> (holder as ShareRowViewHolder).bind(item.share)
    }
  }

  private class SectionHeaderViewHolder(private val binding: ItemSectionHeaderBinding) :
      RecyclerView.ViewHolder(binding.root) {
    fun bind(title: String) {
      binding.textSectionTitle.text = title
    }
  }

  private class InviteRowViewHolder(
      private val binding: ItemInviteLinkBinding,
      private val callbacks: SharingInviteCallbacks,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(invite: ChildInvite) {
      binding.textInviteRole.text = invite.roleDisplay
      binding.chipPaused.visibility =
          if (invite.isActive) android.view.View.GONE else android.view.View.VISIBLE
      binding.textInviteCreated.text = formatCreated(invite.createdAt)
      binding.buttonCopy.setOnClickListener { callbacks.onCopyLink(invite) }
      binding.buttonToggle.text =
          binding.root.context.getString(
              if (invite.isActive) R.string.invite_link_pause else R.string.invite_link_resume)
      binding.buttonToggle.setOnClickListener { callbacks.onToggleInvite(invite) }
      binding.buttonDelete.setOnClickListener { callbacks.onDeleteInvite(invite) }
    }

    private fun formatCreated(iso: String): String {
      return try {
        val inFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
              timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
        val outFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val date = inFormat.parse(iso.replace("Z", "").take(19))
        if (date != null) outFormat.format(date) else iso
      } catch (_: Exception) {
        iso
      }
    }
  }

  private class ShareRowViewHolder(private val binding: ItemSharingBinding) :
      RecyclerView.ViewHolder(binding.root) {
    fun bind(share: ChildShare) {
      binding.textEmail.text = share.userEmail
      binding.textRole.text =
          when (share.role) {
            "co-parent" -> binding.root.context.getString(R.string.sharing_role_co_parent)
            "caregiver" -> binding.root.context.getString(R.string.sharing_role_caregiver)
            else -> share.role
          }
    }
  }

  private object DiffCallback : DiffUtil.ItemCallback<SharingListItem>() {
    override fun areItemsTheSame(
        old: SharingListItem,
        new: SharingListItem,
    ): Boolean =
        when {
          old is SharingListItem.InviteLinkHeader && new is SharingListItem.InviteLinkHeader -> true
          old is SharingListItem.PeopleHeader && new is SharingListItem.PeopleHeader -> true
          old is SharingListItem.InviteRow && new is SharingListItem.InviteRow ->
              old.invite.id == new.invite.id
          old is SharingListItem.ShareRow && new is SharingListItem.ShareRow ->
              old.share.id == new.share.id
          else -> false
        }

    override fun areContentsTheSame(
        old: SharingListItem,
        new: SharingListItem,
    ): Boolean = old == new
  }
}

package net.poopyfeed.pf.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.databinding.ItemNotificationBinding
import net.poopyfeed.pf.util.formatRelativeTime

/**
 * RecyclerView adapter for the notifications list. Shows message, relative time, and read state.
 * Calls [onNotificationClick] when an item is tapped (navigate to child + mark read).
 */
class NotificationAdapter(private val onNotificationClick: (Notification) -> Unit) :
    ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(
        NotificationDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
    val binding =
        ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return NotificationViewHolder(binding, onNotificationClick)
  }

  override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class NotificationViewHolder(
      private val binding: ItemNotificationBinding,
      private val onNotificationClick: (Notification) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(notification: Notification) {
      binding.textMessage.text = notification.message
      binding.textTime.text = formatRelativeTime(binding.root.context, notification.createdAt)
      binding.root.contentDescription =
          binding.root.context.getString(
              net.poopyfeed.pf.R.string.a11y_notification_item,
              notification.message)
      binding.textMeta.text =
          binding.root.context.getString(
              net.poopyfeed.pf.R.string.notification_meta,
              notification.actorName,
              notification.childName)
      binding.dotUnread.visibility = if (notification.isRead) View.GONE else View.VISIBLE
      binding.root.setOnClickListener { onNotificationClick(notification) }
    }
  }

  internal class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
    override fun areItemsTheSame(old: Notification, new: Notification): Boolean = old.id == new.id

    override fun areContentsTheSame(old: Notification, new: Notification): Boolean = old == new
  }
}

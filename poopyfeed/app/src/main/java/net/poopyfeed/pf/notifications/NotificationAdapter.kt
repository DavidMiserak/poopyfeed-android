package net.poopyfeed.pf.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.databinding.ItemNotificationBinding
import net.poopyfeed.pf.util.formatRelativeTime

/**
 * RecyclerView adapter for displaying a list of notifications using Paging 3. Shows notification
 * message, timestamp, actor/child metadata, and unread indicator. Tap triggers
 * [onNotificationClick].
 */
class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit,
) :
    PagingDataAdapter<Notification, NotificationAdapter.NotificationViewHolder>(
        NotificationDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemNotificationBinding.inflate(inflater, parent, false)
    return NotificationViewHolder(binding, onNotificationClick)
  }

  override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
    val notification = getItem(position)
    if (notification != null) {
      holder.bind(notification)
    }
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
              net.poopyfeed.pf.R.string.a11y_notification_item, notification.message)
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
    override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean =
        oldItem == newItem
  }
}

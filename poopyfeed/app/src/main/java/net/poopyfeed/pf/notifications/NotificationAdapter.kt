package net.poopyfeed.pf.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.databinding.ItemNotificationBinding
import net.poopyfeed.pf.databinding.ItemNotificationLoadMoreBinding
import net.poopyfeed.pf.util.formatRelativeTime

/**
 * List item for the notifications RecyclerView: either a notification row or the load-more footer.
 */
sealed class NotificationsListItem {
  data class NotificationItem(val notification: Notification) : NotificationsListItem()

  data class LoadMoreFooter(val isLoading: Boolean) : NotificationsListItem()
}

private const val VIEW_TYPE_NOTIFICATION = 0
private const val VIEW_TYPE_LOAD_MORE = 1

/**
 * RecyclerView adapter for the notifications list. Shows notification rows and an optional
 * load-more footer. Calls [onNotificationClick] when a notification is tapped and [onLoadMoreClick]
 * when the footer "Load more" is tapped (when not loading).
 */
class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit,
    private val onLoadMoreClick: () -> Unit,
) : ListAdapter<NotificationsListItem, RecyclerView.ViewHolder>(NotificationsListDiffCallback()) {

  override fun getItemViewType(position: Int): Int =
      when (getItem(position)) {
        is NotificationsListItem.NotificationItem -> VIEW_TYPE_NOTIFICATION
        is NotificationsListItem.LoadMoreFooter -> VIEW_TYPE_LOAD_MORE
      }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      when (viewType) {
        VIEW_TYPE_NOTIFICATION -> {
          val binding =
              ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
          NotificationViewHolder(binding, onNotificationClick)
        }
        VIEW_TYPE_LOAD_MORE -> {
          val binding =
              ItemNotificationLoadMoreBinding.inflate(
                  LayoutInflater.from(parent.context), parent, false)
          LoadMoreFooterViewHolder(binding, onLoadMoreClick)
        }
        else -> error("Unknown viewType: $viewType")
      }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (val item = getItem(position)) {
      is NotificationsListItem.NotificationItem ->
          (holder as NotificationViewHolder).bind(item.notification)
      is NotificationsListItem.LoadMoreFooter ->
          (holder as LoadMoreFooterViewHolder).bind(item.isLoading)
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

  class LoadMoreFooterViewHolder(
      private val binding: ItemNotificationLoadMoreBinding,
      private val onLoadMoreClick: () -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(isLoading: Boolean) {
      binding.buttonLoadMore.visibility = if (isLoading) View.GONE else View.VISIBLE
      binding.progressLoadMore.visibility = if (isLoading) View.VISIBLE else View.GONE
      binding.buttonLoadMore.setOnClickListener { if (!isLoading) onLoadMoreClick() }
    }
  }

  private class NotificationsListDiffCallback : DiffUtil.ItemCallback<NotificationsListItem>() {
    override fun areItemsTheSame(
        oldItem: NotificationsListItem,
        newItem: NotificationsListItem,
    ): Boolean =
        when {
          oldItem is NotificationsListItem.NotificationItem &&
              newItem is NotificationsListItem.NotificationItem ->
              oldItem.notification.id == newItem.notification.id
          oldItem is NotificationsListItem.LoadMoreFooter &&
              newItem is NotificationsListItem.LoadMoreFooter -> true
          else -> false
        }

    override fun areContentsTheSame(
        oldItem: NotificationsListItem,
        newItem: NotificationsListItem,
    ): Boolean =
        when {
          oldItem is NotificationsListItem.NotificationItem &&
              newItem is NotificationsListItem.NotificationItem ->
              oldItem.notification == newItem.notification
          oldItem is NotificationsListItem.LoadMoreFooter &&
              newItem is NotificationsListItem.LoadMoreFooter ->
              oldItem.isLoading == newItem.isLoading
          else -> false
        }
  }
}

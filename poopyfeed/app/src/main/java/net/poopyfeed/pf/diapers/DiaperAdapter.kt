package net.poopyfeed.pf.diapers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.databinding.ItemDiaperBinding
import net.poopyfeed.pf.util.formatRelativeTime
import net.poopyfeed.pf.util.formatTimeForDisplayWithTimezone

/**
 * RecyclerView adapter for displaying a list of diaper changes. Shows change type (Wet/Dirty/Both)
 * and relative time with exact timestamp in the user's profile timezone. Tap triggers
 * [onItemClick]; long-press triggers [onDeleteClick].
 */
class DiaperAdapter(
    private val profileTimezoneId: String?,
    private val onItemClick: (Diaper) -> Unit,
    private val onDeleteClick: (Diaper) -> Unit,
) : PagingDataAdapter<Diaper, DiaperAdapter.DiaperViewHolder>(DiaperDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaperViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemDiaperBinding.inflate(inflater, parent, false)
    return DiaperViewHolder(binding, profileTimezoneId, onItemClick, onDeleteClick)
  }

  override fun onBindViewHolder(holder: DiaperViewHolder, position: Int) {
    val diaper = getItem(position)
    if (diaper != null) {
      holder.bind(diaper)
    }
  }

  class DiaperViewHolder(
      private val binding: ItemDiaperBinding,
      private val profileTimezoneId: String?,
      private val onItemClick: (Diaper) -> Unit,
      private val onDeleteClick: (Diaper) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(diaper: Diaper) {
      val ctx = binding.root.context
      val typeLabel =
          when (diaper.change_type.lowercase()) {
            "wet" -> ctx.getString(R.string.diaper_change_wet)
            "dirty" -> ctx.getString(R.string.diaper_change_dirty)
            "both" -> ctx.getString(R.string.diaper_change_both)
            else -> diaper.change_type.replaceFirstChar { it.uppercaseChar() }
          }
      binding.textChangeType.text = typeLabel
      val relativeTime = formatRelativeTime(ctx, diaper.timestamp)
      val absoluteTime =
          formatTimeForDisplayWithTimezone(
              ctx,
              diaper.timestamp,
              profileTimezoneId,
          )
      val timeSummary = "$relativeTime \u2022 $absoluteTime"
      binding.textTime.text = timeSummary
      binding.textSavedLocally.visibility = if (diaper.id < 0) View.VISIBLE else View.GONE
      binding.root.contentDescription = ctx.getString(R.string.a11y_diaper_item, timeSummary)
      binding.root.setOnClickListener { onItemClick(diaper) }
      binding.root.setOnLongClickListener {
        onDeleteClick(diaper)
        true
      }
    }
  }

  internal class DiaperDiffCallback : DiffUtil.ItemCallback<Diaper>() {
    override fun areItemsTheSame(oldItem: Diaper, newItem: Diaper): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Diaper, newItem: Diaper): Boolean = oldItem == newItem
  }
}

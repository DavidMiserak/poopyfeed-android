package net.poopyfeed.pf.diapers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.databinding.ItemDiaperBinding
import net.poopyfeed.pf.util.formatRelativeTime

/**
 * RecyclerView adapter for displaying a list of diaper changes. Shows change type (Wet/Dirty/Both)
 * and relative time. Long-press triggers [onDeleteClick].
 */
class DiaperAdapter(private val onDeleteClick: (Diaper) -> Unit) :
    ListAdapter<Diaper, DiaperAdapter.DiaperViewHolder>(DiaperDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaperViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemDiaperBinding.inflate(inflater, parent, false)
    return DiaperViewHolder(binding, onDeleteClick)
  }

  override fun onBindViewHolder(holder: DiaperViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class DiaperViewHolder(
      private val binding: ItemDiaperBinding,
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
      val timeSummary = formatRelativeTime(ctx, diaper.timestamp)
      binding.textTime.text = timeSummary
      binding.root.contentDescription = ctx.getString(R.string.a11y_diaper_item, timeSummary)
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

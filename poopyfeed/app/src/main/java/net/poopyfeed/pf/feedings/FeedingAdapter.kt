package net.poopyfeed.pf.feedings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.databinding.ItemFeedingBinding
import net.poopyfeed.pf.util.formatRelativeTime

/**
 * RecyclerView adapter for displaying a list of feedings. Shows type (Bottle/Breast); for bottle
 * shows amount (oz), for breast shows duration and side when available (e.g. "15 min • Left"). Tap
 * triggers [onItemClick]; long-press triggers [onDeleteClick].
 */
class FeedingAdapter(
    private val onItemClick: (Feeding) -> Unit,
    private val onDeleteClick: (Feeding) -> Unit,
) : ListAdapter<Feeding, FeedingAdapter.FeedingViewHolder>(FeedingDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedingViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemFeedingBinding.inflate(inflater, parent, false)
    return FeedingViewHolder(binding, onItemClick, onDeleteClick)
  }

  override fun onBindViewHolder(holder: FeedingViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class FeedingViewHolder(
      private val binding: ItemFeedingBinding,
      private val onItemClick: (Feeding) -> Unit,
      private val onDeleteClick: (Feeding) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(feeding: Feeding) {
      val ctx = binding.root.context
      val typeLabel =
          when (feeding.feeding_type.lowercase()) {
            "bottle" -> ctx.getString(R.string.feeding_type_bottle)
            "breast" -> ctx.getString(R.string.feeding_type_breast)
            else -> feeding.feeding_type.replaceFirstChar { it.uppercaseChar() }
          }
      binding.textFeedingType.text = typeLabel
      when (feeding.feeding_type.lowercase()) {
        "bottle" -> {
          if (feeding.amount_oz != null) {
            binding.textAmount.visibility = View.VISIBLE
            binding.textAmount.text = "${feeding.amount_oz} oz"
          } else {
            binding.textAmount.visibility = View.GONE
          }
        }
        "breast" -> {
          val detail = formatBreastDetail(ctx, feeding)
          if (detail != null) {
            binding.textAmount.visibility = View.VISIBLE
            binding.textAmount.text = detail
          } else {
            binding.textAmount.visibility = View.GONE
          }
        }
        else -> binding.textAmount.visibility = View.GONE
      }
      val timeSummary = formatRelativeTime(ctx, feeding.timestamp)
      binding.textTime.text = timeSummary
      binding.root.contentDescription = ctx.getString(R.string.a11y_feeding_item, timeSummary)
      binding.root.setOnClickListener { onItemClick(feeding) }
      binding.root.setOnLongClickListener {
        onDeleteClick(feeding)
        true
      }
    }

    private fun formatBreastDetail(ctx: android.content.Context, feeding: Feeding): String? {
      val duration = feeding.duration_minutes
      val sideLabel =
          feeding.side?.lowercase()?.let { side ->
            when (side) {
              "left" -> ctx.getString(R.string.create_feeding_side_left)
              "right" -> ctx.getString(R.string.create_feeding_side_right)
              "both" -> ctx.getString(R.string.create_feeding_side_both)
              else -> null
            }
          }
      return when {
        duration != null && sideLabel != null ->
            ctx.getString(R.string.feeding_breast_detail, duration, sideLabel)
        duration != null -> ctx.getString(R.string.feeding_breast_duration_only, duration)
        sideLabel != null -> sideLabel
        else -> null
      }
    }
  }

  internal class FeedingDiffCallback : DiffUtil.ItemCallback<Feeding>() {
    override fun areItemsTheSame(oldItem: Feeding, newItem: Feeding): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Feeding, newItem: Feeding): Boolean =
        oldItem == newItem
  }
}

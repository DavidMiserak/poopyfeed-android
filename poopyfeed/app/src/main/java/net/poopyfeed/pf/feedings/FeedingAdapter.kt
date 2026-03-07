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
 * RecyclerView adapter for displaying a list of feedings. Shows type (Bottle/Breast), amount for
 * bottle, and relative time. Long-press triggers [onDeleteClick].
 */
class FeedingAdapter(private val onDeleteClick: (Feeding) -> Unit) :
    ListAdapter<Feeding, FeedingAdapter.FeedingViewHolder>(FeedingDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedingViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemFeedingBinding.inflate(inflater, parent, false)
    return FeedingViewHolder(binding, onDeleteClick)
  }

  override fun onBindViewHolder(holder: FeedingViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class FeedingViewHolder(
      private val binding: ItemFeedingBinding,
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
      if (feeding.amount_oz != null) {
        binding.textAmount.visibility = View.VISIBLE
        binding.textAmount.text = "${feeding.amount_oz} oz"
      } else {
        binding.textAmount.visibility = View.GONE
      }
      val timeSummary = formatRelativeTime(ctx, feeding.timestamp)
      binding.textTime.text = timeSummary
      binding.root.contentDescription = ctx.getString(R.string.a11y_feeding_item, timeSummary)
      binding.root.setOnLongClickListener {
        onDeleteClick(feeding)
        true
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

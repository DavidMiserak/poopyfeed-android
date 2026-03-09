package net.poopyfeed.pf.ui.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import net.poopyfeed.pf.data.models.TimelineEvent
import net.poopyfeed.pf.databinding.ItemTimelineEventBinding
import net.poopyfeed.pf.databinding.ItemTimelineGapBinding
import net.poopyfeed.pf.util.formatTimeForDisplay

/**
 * RecyclerView adapter for timeline items. Renders events with emoji, summary, local time, and
 * color-coded accent stripe, plus gap indicators between events with significant time gaps.
 */
class TimelineAdapter :
    ListAdapter<TimelineItem, RecyclerView.ViewHolder>(TimelineItemDiffCallback()) {

  companion object {
    private const val VIEW_TYPE_EVENT = 0
    private const val VIEW_TYPE_GAP = 1
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is TimelineItem.Event -> VIEW_TYPE_EVENT
      is TimelineItem.Gap -> VIEW_TYPE_GAP
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      VIEW_TYPE_GAP -> {
        val binding = ItemTimelineGapBinding.inflate(inflater, parent, false)
        GapViewHolder(binding)
      }
      else -> {
        val binding = ItemTimelineEventBinding.inflate(inflater, parent, false)
        EventViewHolder(binding)
      }
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (val item = getItem(position)) {
      is TimelineItem.Event -> (holder as EventViewHolder).bind(item.event)
      is TimelineItem.Gap -> (holder as GapViewHolder).bind(item.durationMinutes)
    }
  }

  /** ViewHolder for a single timeline event. */
  class EventViewHolder(private val binding: ItemTimelineEventBinding) :
      RecyclerView.ViewHolder(binding.root) {

    fun bind(event: TimelineEvent) {
      val context = binding.root.context
      val (emoji, summary) = formatEvent(context, event)

      binding.eventEmoji.text = emoji
      binding.eventSummary.text = summary
      binding.eventTime.text = formatTimeForDisplay(context, event.at)

      // Set accent stripe color based on event type
      val stripeColor =
          when {
            event.feeding != null ->
                MaterialColors.getColor(binding.root, MaterialR.attr.colorPrimary)
            event.diaper != null ->
                MaterialColors.getColor(binding.root, MaterialR.attr.colorSecondary)
            event.nap != null -> MaterialColors.getColor(binding.root, MaterialR.attr.colorTertiary)
            else -> MaterialColors.getColor(binding.root, MaterialR.attr.colorOutline)
          }
      binding.accentStripe.setBackgroundColor(stripeColor)
    }

    private fun formatEvent(context: Context, event: TimelineEvent): Pair<String, String> {
      return when {
        event.feeding != null -> {
          val feeding = event.feeding
          val emoji =
              when (feeding.feedingType) {
                "bottle" -> "🍼"
                "breast" -> "🤱"
                else -> "🍼"
              }
          val summary =
              when (feeding.feedingType) {
                "bottle" -> {
                  val amt = feeding.amountOz ?: "—"
                  "Bottle - ${amt} oz"
                }
                "breast" -> {
                  val duration = feeding.durationMinutes?.let { "${it}m" } ?: "—"
                  val side = feeding.side?.let { " ($it)" } ?: ""
                  "Breastfeed - ${duration}${side}"
                }
                else -> "Feeding"
              }
          emoji to summary
        }
        event.diaper != null -> {
          val diaper = event.diaper
          val emoji =
              when {
                diaper.changeType.contains("poop", ignoreCase = true) &&
                    diaper.changeType.contains("wet", ignoreCase = true) -> "🔄"
                diaper.changeType.contains("poop", ignoreCase = true) -> "💩"
                diaper.changeType.contains("wet", ignoreCase = true) -> "💧"
                else -> "🔄"
              }
          val summary =
              when {
                diaper.changeType.contains("poop", ignoreCase = true) &&
                    diaper.changeType.contains("wet", ignoreCase = true) -> "Wet & dirty diaper"
                diaper.changeType.contains("poop", ignoreCase = true) -> "Dirty diaper"
                diaper.changeType.contains("wet", ignoreCase = true) -> "Wet diaper"
                else -> "Diaper change"
              }
          emoji to summary
        }
        event.nap != null -> {
          val nap = event.nap
          val emoji = "😴"
          val duration =
              if (nap.endedAt != null && nap.durationMinutes != null) {
                val mins = nap.durationMinutes
                val hours = mins / 60
                val remainingMins = mins % 60
                when {
                  hours > 0 && remainingMins > 0 -> "${hours}h ${remainingMins}m"
                  hours > 0 -> "${hours}h"
                  else -> "${mins}m"
                }
              } else {
                "ongoing"
              }
          val summary = "Nap - $duration"
          emoji to summary
        }
        else -> "❓" to "Unknown event"
      }
    }
  }

  /** ViewHolder for a time gap indicator. */
  class GapViewHolder(private val binding: ItemTimelineGapBinding) :
      RecyclerView.ViewHolder(binding.root) {

    fun bind(durationMinutes: Long) {
      val hours = durationMinutes / 60
      val mins = durationMinutes % 60
      val label =
          when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m gap"
            hours > 0 -> "${hours}h gap"
            else -> "${mins}m gap"
          }
      binding.textGapDuration.text = label
    }
  }

  private class TimelineItemDiffCallback : DiffUtil.ItemCallback<TimelineItem>() {
    override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
      return when {
        oldItem is TimelineItem.Event && newItem is TimelineItem.Event ->
            oldItem.event.at == newItem.event.at && oldItem.event.type == newItem.event.type
        oldItem is TimelineItem.Gap && newItem is TimelineItem.Gap ->
            oldItem.durationMinutes == newItem.durationMinutes
        else -> false
      }
    }

    override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
      return oldItem == newItem
    }
  }
}

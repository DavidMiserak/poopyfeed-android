package net.poopyfeed.pf.ui.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.data.models.TimelineEvent
import net.poopyfeed.pf.databinding.ItemTimelineEventBinding

/**
 * RecyclerView adapter for timeline events. Displays feeding, diaper, and nap events with emoji
 * icon, summary, and local time.
 */
class TimelineAdapter : ListAdapter<TimelineEvent, TimelineAdapter.ViewHolder>(DiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding =
        ItemTimelineEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  /** ViewHolder for a single timeline event. */
  class ViewHolder(private val binding: ItemTimelineEventBinding) :
      RecyclerView.ViewHolder(binding.root) {

    fun bind(event: TimelineEvent) {
      val context = binding.root.context
      val (emoji, summary) = formatEvent(context, event)
      val timeStr = extractTime(event.at)

      binding.eventEmoji.text = emoji
      binding.eventSummary.text = summary
      binding.eventTime.text = timeStr
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

    private fun extractTime(isoString: String): String {
      return try {
        // isoString format: "2026-03-09T14:30:00Z"
        val timePart = isoString.substringAfter("T").substringBefore("Z")
        timePart.substringBeforeLast(":") // "14:30"
      } catch (e: Exception) {
        "—"
      }
    }
  }

  private class DiffCallback : DiffUtil.ItemCallback<TimelineEvent>() {
    override fun areItemsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
      return oldItem.at == newItem.at && oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
      return oldItem == newItem
    }
  }
}

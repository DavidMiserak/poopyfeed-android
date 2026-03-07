package net.poopyfeed.pf.children

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.databinding.ItemChildBinding
import net.poopyfeed.pf.util.formatAge

/**
 * RecyclerView adapter for displaying a list of children. Shows child name, age, gender, and last
 * activity summary. Calls [onChildClick] when a child is tapped.
 */
class ChildAdapter(private val onChildClick: (Child) -> Unit) :
    ListAdapter<Child, ChildAdapter.ChildViewHolder>(ChildDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemChildBinding.inflate(inflater, parent, false)
    return ChildViewHolder(binding, onChildClick)
  }

  override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class ChildViewHolder(
      private val binding: ItemChildBinding,
      private val onChildClick: (Child) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(child: Child) {
      binding.textChildName.text = child.name

      // Age and gender line: "X months • Girl" or "X yr Y mo • Boy"
      val ageFormatted = formatAge(child.date_of_birth)
      val genderFormatted =
          if (child.gender == "F")
              binding.root.context.getString(R.string.create_child_gender_female)
          else binding.root.context.getString(R.string.create_child_gender_male)
      binding.textAgeGender.text = "$ageFormatted • $genderFormatted"

      // Parse activity timestamps
      val feedingMs = if (child.last_feeding != null) {
        try {
          java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
              .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
              .parse(child.last_feeding)?.time
        } catch (e: Exception) { null }
      } else null

      val diaperMs = if (child.last_diaper_change != null) {
        try {
          java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
              .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
              .parse(child.last_diaper_change)?.time
        } catch (e: Exception) { null }
      } else null

      val napMs = if (child.last_nap != null) {
        try {
          java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
              .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
              .parse(child.last_nap)?.time
        } catch (e: Exception) { null }
      } else null

      // Activity times on right column (just the time, emoji + label are in layout)
      binding.textLastFeeding.text = formatTimeAbbreviated(feedingMs)
      binding.textLastDiaper.text = formatTimeAbbreviated(diaperMs)
      binding.textLastNap.text = formatTimeAbbreviated(napMs)

      // Show role badge only for non-owners
      binding.chipRole.visibility = if (child.user_role == "owner") View.GONE else View.VISIBLE
      if (child.user_role != "owner") {
        binding.chipRole.text = child.user_role.replaceFirstChar { it.uppercaseChar() }
      }

      // TalkBack: describe card for accessibility
      binding.root.contentDescription =
          binding.root.context.getString(R.string.a11y_child_card, child.name)

      // Click listener
      binding.root.setOnClickListener { onChildClick(child) }
    }

    private fun formatTimeAbbreviated(timeMs: Long?): String {
      if (timeMs == null) return "—"
      val now = System.currentTimeMillis()
      val diffMs = now - timeMs
      val diffMinutes = diffMs / 60000
      val diffHours = diffMs / 3600000
      val diffDays = diffMs / 86400000

      return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m"
        diffHours < 24 -> "${diffHours}h"
        else -> "${diffDays}d"
      }
    }
  }

  internal class ChildDiffCallback : DiffUtil.ItemCallback<Child>() {
    override fun areItemsTheSame(oldItem: Child, newItem: Child): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Child, newItem: Child): Boolean {
      return oldItem == newItem
    }
  }
}

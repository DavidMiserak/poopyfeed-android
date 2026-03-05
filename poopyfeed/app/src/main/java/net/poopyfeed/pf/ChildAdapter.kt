package net.poopyfeed.pf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.databinding.ItemChildBinding

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

      // Last activities: show all three with label and time on separate lines
      val feedingTime = formatRelativeTime(binding.root.context, child.last_feeding)
      binding.textLastFeeding.text = "Last feeding\n$feedingTime"

      val diaperTime = formatRelativeTime(binding.root.context, child.last_diaper_change)
      binding.textLastDiaper.text = "Last diaper\n$diaperTime"

      val napTime = formatRelativeTime(binding.root.context, child.last_nap)
      binding.textLastNap.text = "Last nap\n$napTime"

      // Show role badge only for non-owners
      binding.chipRole.visibility = if (child.user_role == "owner") View.GONE else View.VISIBLE
      if (child.user_role != "owner") {
        binding.chipRole.text = child.user_role.replaceFirstChar { it.uppercaseChar() }
      }

      // Click listener
      binding.root.setOnClickListener { onChildClick(child) }
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

package net.poopyfeed.pf.children

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
import net.poopyfeed.pf.util.formatRelativeTimeShort

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
      val ctx = binding.root.context
      val genderFormatted =
          when (child.gender) {
            "F" -> ctx.getString(R.string.create_child_gender_female)
            "O" -> ctx.getString(R.string.create_child_gender_other)
            else -> ctx.getString(R.string.create_child_gender_male)
          }
      binding.textAgeGender.text =
          ctx.getString(R.string.children_list_age_gender, ageFormatted, genderFormatted)

      // Activity times on right column: short format ("1m", "1h", "1d")
      binding.textLastFeeding.text = formatRelativeTimeShort(ctx, child.last_feeding)
      binding.textLastDiaper.text = formatRelativeTimeShort(ctx, child.last_diaper_change)
      binding.textLastNap.text = formatRelativeTimeShort(ctx, child.last_nap)

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

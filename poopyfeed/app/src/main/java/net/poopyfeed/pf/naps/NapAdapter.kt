package net.poopyfeed.pf.naps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.databinding.ItemNapBinding
import net.poopyfeed.pf.util.formatNapDuration
import net.poopyfeed.pf.util.formatRelativeTime

/**
 * RecyclerView adapter for displaying a list of naps. Shows start time, duration or "In progress",
 * and "End Nap" button when [Nap.end_time] is null. Long-press triggers [onDeleteClick].
 */
class NapAdapter(
    private val onDeleteClick: (Nap) -> Unit,
    private val onEndNapClick: (Nap) -> Unit,
) : ListAdapter<Nap, NapAdapter.NapViewHolder>(NapDiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NapViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ItemNapBinding.inflate(inflater, parent, false)
    return NapViewHolder(binding, onDeleteClick, onEndNapClick)
  }

  override fun onBindViewHolder(holder: NapViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class NapViewHolder(
      private val binding: ItemNapBinding,
      private val onDeleteClick: (Nap) -> Unit,
      private val onEndNapClick: (Nap) -> Unit,
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(nap: Nap) {
      val ctx = binding.root.context
      binding.textStartTime.text = formatRelativeTime(ctx, nap.start_time)
      if (nap.end_time == null) {
        binding.textDuration.visibility = View.GONE
        binding.labelInProgress.visibility = View.VISIBLE
        binding.buttonEndNap.visibility = View.VISIBLE
        binding.buttonEndNap.setOnClickListener { onEndNapClick(nap) }
      } else {
        binding.textDuration.visibility = View.VISIBLE
        binding.labelInProgress.visibility = View.GONE
        binding.buttonEndNap.visibility = View.GONE
        binding.textDuration.text = formatNapDuration(ctx, nap.start_time, nap.end_time)
      }
      binding.root.setOnLongClickListener {
        onDeleteClick(nap)
        true
      }
    }
  }

  internal class NapDiffCallback : DiffUtil.ItemCallback<Nap>() {
    override fun areItemsTheSame(oldItem: Nap, newItem: Nap): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Nap, newItem: Nap): Boolean = oldItem == newItem
  }
}

package com.example.questgen.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.questgen.data.model.Challenge
import com.example.questgen.databinding.ItemHistoryBinding
import com.example.questgen.util.formatCoins

class HistoryAdapter(
    private var entries: List<Challenge>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val challenge = entries[position]
        holder.binding.tvHistoryTitle.text = challenge.title

        // Format metadata text and color based on challenge status
        when (challenge.status) {
            "COMPLETED" -> {
                holder.binding.tvHistoryMeta.text = "+${challenge.reward_amount.formatCoins()} (Concluído)"
                holder.binding.tvHistoryMeta.setTextColor(Color.parseColor("#39FF14")) // Neon Green
                holder.binding.imgHistoryStatusIndicator.setColorFilter(Color.parseColor("#39FF14"))
            }
            "FAILED" -> {
                holder.binding.tvHistoryMeta.text = "-50 GC (Tempo Esgotado)"
                holder.binding.tvHistoryMeta.setTextColor(Color.parseColor("#FF4C4C")) // Neon Red
                holder.binding.imgHistoryStatusIndicator.setColorFilter(Color.parseColor("#FF4C4C"))
            }
            "FORFEITED" -> {
                holder.binding.tvHistoryMeta.text = "-50 GC (Desistiu)"
                holder.binding.tvHistoryMeta.setTextColor(Color.parseColor("#FF4C4C")) // Neon Red
                holder.binding.imgHistoryStatusIndicator.setColorFilter(Color.parseColor("#FF4C4C"))
            }
            else -> {
                holder.binding.tvHistoryMeta.text = challenge.status
                holder.binding.tvHistoryMeta.setTextColor(Color.WHITE)
                holder.binding.imgHistoryStatusIndicator.clearColorFilter()
            }
        }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<Challenge>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}

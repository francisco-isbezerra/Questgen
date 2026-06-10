package com.example.questgen.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.questgen.databinding.ItemHistoryBinding

class HistoryAdapter(
    private var entries: List<Pair<String, String>>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = entries[position]
        holder.binding.tvHistoryTitle.text = entry.first
        holder.binding.tvHistoryMeta.text = entry.second
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<Pair<String, String>>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}

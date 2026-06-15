package com.example.questgen.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.Challenge
import com.example.questgen.databinding.ItemPendingChallengeBinding
import com.example.questgen.util.formatCoins

class PendingChallengesAdapter(
    private var entries: List<Challenge>
) : RecyclerView.Adapter<PendingChallengesAdapter.PendingViewHolder>() {

    inner class PendingViewHolder(val binding: ItemPendingChallengeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingViewHolder {
        val binding = ItemPendingChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
        val challenge = entries[position]
        holder.binding.tvPendingTitle.text = challenge.title
        holder.binding.tvPendingReward.text = "+${challenge.reward_amount.formatCoins()}"

        // Load proof image using Coil if present
        if (!challenge.imagem_comprovante.isNullOrEmpty()) {
            val imageUrl = "http://192.168.15.143/ApiQuestGen/uploads_comprovantes/${challenge.imagem_comprovante}"
            holder.binding.imgProofImage.imageTintList = null
            holder.binding.imgProofImage.colorFilter = null
            holder.binding.imgProofImage.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_games)
                error(R.drawable.ic_games)
            }
        } else {
            holder.binding.imgProofImage.setImageResource(R.drawable.ic_games)
            holder.binding.imgProofImage.setColorFilter(holder.itemView.context.getColor(R.color.gray_text))
        }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<Challenge>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}

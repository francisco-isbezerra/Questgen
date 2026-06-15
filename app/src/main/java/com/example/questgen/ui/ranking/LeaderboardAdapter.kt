package com.example.questgen.ui.ranking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.LeaderboardRow
import com.example.questgen.databinding.ItemRankingBinding
import com.example.questgen.util.formatCoins

class LeaderboardAdapter(
    private var rows: List<LeaderboardRow>
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    inner class LeaderboardViewHolder(val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val row = rows[position]
        
        holder.binding.tvRowPosition.text = "${row.position}º"
        holder.binding.tvRowName.text = row.name
        holder.binding.tvRowCoins.text = row.game_coins.formatCoins()

        // Apply neon border based on moldura_neon
        val density = holder.itemView.resources.displayMetrics.density
        if (!row.moldura_neon.isNullOrEmpty()) {
            val borderColor = when (row.moldura_neon) {
                "neon_ciano" -> "#00BFFF"
                "neon_roxo" -> "#8A2BE2"
                "ouro_lendario" -> "#FFD700"
                else -> null
            }
            if (borderColor != null) {
                holder.binding.cardRowAvatar.setCardBackgroundColor(android.graphics.Color.parseColor(borderColor))
                val borderPadding = (2.5 * density).toInt()
                holder.binding.imgRowAvatar.setPadding(borderPadding, borderPadding, borderPadding, borderPadding)
            } else {
                holder.binding.cardRowAvatar.setCardBackgroundColor(android.graphics.Color.parseColor("#141414"))
            }
        } else {
            holder.binding.cardRowAvatar.setCardBackgroundColor(android.graphics.Color.parseColor("#141414"))
        }

        // Load avatar image dynamically via Coil
        val absoluteUrl = row.getAbsoluteImageUrl()
        if (!absoluteUrl.isNullOrEmpty()) {
            holder.binding.imgRowAvatar.imageTintList = null
            holder.binding.imgRowAvatar.colorFilter = null
            if (row.moldura_neon.isNullOrEmpty()) {
                holder.binding.imgRowAvatar.setPadding(0, 0, 0, 0)
            }
            holder.binding.imgRowAvatar.load(absoluteUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
                memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                diskCachePolicy(coil.request.CachePolicy.DISABLED)
            }
        } else {
            val paddingPx = if (!row.moldura_neon.isNullOrEmpty()) (4 * density).toInt() else (3 * density).toInt()
            holder.binding.imgRowAvatar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            holder.binding.imgRowAvatar.imageTintList = android.content.res.ColorStateList.valueOf(holder.itemView.resources.getColor(R.color.gray_text, null))
            holder.binding.imgRowAvatar.setImageResource(R.drawable.ic_profile)
        }

        // Highlight if this row is the current player
        if (row.is_current_user) {
            holder.binding.tvRowYouTag.visibility = View.VISIBLE
            holder.binding.tvRowName.text = "${row.name} (VOCÊ)"
            holder.binding.cardLeaderboardRow.setCardBackgroundColor(holder.itemView.resources.getColor(R.color.bg_card, null))
            holder.binding.cardLeaderboardRow.strokeColor = holder.itemView.resources.getColor(R.color.azul_neon, null)
            holder.binding.cardLeaderboardRow.strokeWidth = 3
        } else {
            holder.binding.tvRowYouTag.visibility = View.GONE
            holder.binding.cardLeaderboardRow.setCardBackgroundColor(holder.itemView.resources.getColor(R.color.bg_secundario, null))
            holder.binding.cardLeaderboardRow.strokeWidth = 0
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateData(newRows: List<LeaderboardRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}

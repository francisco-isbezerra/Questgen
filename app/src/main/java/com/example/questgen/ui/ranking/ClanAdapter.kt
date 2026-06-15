package com.example.questgen.ui.ranking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.Clan
import com.example.questgen.databinding.ItemClanBinding

class ClanAdapter(private var list: List<Clan>) : RecyclerView.Adapter<ClanAdapter.ClanViewHolder>() {

    inner class ClanViewHolder(private val binding: ItemClanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(clan: Clan, position: Int) {
            binding.tvClanRank.text = (position + 1).toString()
            binding.tvClanTag.text = "[${clan.tag.uppercase()}]"
            binding.tvClanName.text = clan.name
            binding.tvClanXp.text = "${clan.xp_total} XP"

            if (!clan.logo_url.isNullOrEmpty()) {
                binding.imgClanLogo.imageTintList = null
                binding.imgClanLogo.colorFilter = null
                binding.imgClanLogo.load(clan.logo_url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_ranking)
                    error(R.drawable.ic_ranking)
                }
            } else {
                binding.imgClanLogo.imageTintList = android.content.res.ColorStateList.valueOf(
                    binding.root.context.resources.getColor(R.color.azul_neon, null)
                )
                binding.imgClanLogo.setImageResource(R.drawable.ic_ranking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClanViewHolder {
        val binding = ItemClanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClanViewHolder, position: Int) {
        holder.bind(list[position], position)
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Clan>) {
        list = newList
        notifyDataSetChanged()
    }
}

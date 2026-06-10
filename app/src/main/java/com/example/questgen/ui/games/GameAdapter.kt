package com.example.questgen.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.Game
import com.example.questgen.databinding.ItemGameBinding

class GameAdapter(
    private var games: List<Game>,
    private val onGameClick: (Game) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    inner class GameViewHolder(val binding: ItemGameBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        holder.binding.tvGameTitle.text = game.title
        holder.binding.tvGameCategory.text = game.category ?: "Arena"
        
        // Dynamic mock player level details aligned right
        val mockLvl = (position * 37 + 12) % 150 + 5
        holder.binding.tvGameLvl.text = "LVL $mockLvl"

        // Load image using Coil
        holder.binding.imgGameBg.load(game.image_url) {
            crossfade(true)
            placeholder(R.color.bg_secundario)
            error(R.color.bg_secundario)
        }

        holder.itemView.setOnClickListener {
            onGameClick(game)
        }
    }

    override fun getItemCount(): Int = games.size

    fun updateData(newGames: List<Game>) {
        games = newGames
        notifyDataSetChanged()
    }
}

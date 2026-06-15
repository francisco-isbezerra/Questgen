package com.example.questgen.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.questgen.R
import com.example.questgen.data.model.BattlePassReward
import com.example.questgen.databinding.ItemBattlePassRewardBinding

class BattlePassAdapter(
    private var rewards: List<BattlePassReward>,
    private var userLevel: Int,
    private var isUserPremium: Boolean,
    private val onClaimClick: (BattlePassReward) -> Unit
) : RecyclerView.Adapter<BattlePassAdapter.BattlePassViewHolder>() {

    inner class BattlePassViewHolder(val binding: ItemBattlePassRewardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BattlePassViewHolder {
        val binding = ItemBattlePassRewardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BattlePassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BattlePassViewHolder, position: Int) {
        val reward = rewards[position]
        val context = holder.itemView.context

        holder.binding.tvRewardLevel.text = "NÍVEL ${reward.level}"
        holder.binding.tvRewardTitle.text = reward.title
        holder.binding.tvRewardDesc.text = reward.description

        if (reward.isPremium) {
            holder.binding.tvRewardTier.text = "★ PREMIUM"
            holder.binding.tvRewardTier.setTextColor(context.resources.getColor(R.color.raridade_lendario, null))
            holder.binding.tvRewardTier.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#2E2800")
            )
            holder.binding.tvRewardLevel.setTextColor(context.resources.getColor(R.color.raridade_lendario, null))
            holder.binding.tvRewardLevel.setBackgroundColor(android.graphics.Color.parseColor("#2E2800"))
        } else {
            holder.binding.tvRewardTier.text = "◆ GRATUITO"
            holder.binding.tvRewardTier.setTextColor(context.resources.getColor(R.color.azul_neon, null))
            holder.binding.tvRewardTier.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#122438")
            )
            holder.binding.tvRewardLevel.setTextColor(context.resources.getColor(R.color.azul_neon, null))
            holder.binding.tvRewardLevel.setBackgroundColor(android.graphics.Color.parseColor("#122438"))
        }

        // Determine State:
        // 1. Claimed
        // 2. Locked: level not met OR (premium item and user not premium)
        // 3. Claimable: level met AND (free item OR premium user) AND not claimed yet
        val isLocked = reward.level > userLevel || (reward.isPremium && !isUserPremium)

        when {
            reward.isClaimed -> {
                holder.binding.tvClaimedLabel.visibility = View.VISIBLE
                holder.binding.layoutLocked.visibility = View.GONE
                holder.binding.btnClaimReward.visibility = View.GONE
                // Dim the card slightly for claimed items
                holder.itemView.alpha = 0.75f
            }
            isLocked -> {
                holder.binding.tvClaimedLabel.visibility = View.GONE
                holder.binding.layoutLocked.visibility = View.VISIBLE
                holder.binding.btnClaimReward.visibility = View.GONE
                // Dim locked items
                holder.itemView.alpha = 0.55f
            }
            else -> {
                // Claimable — full brightness, neon accent
                holder.binding.tvClaimedLabel.visibility = View.GONE
                holder.binding.layoutLocked.visibility = View.GONE
                holder.binding.btnClaimReward.visibility = View.VISIBLE
                holder.itemView.alpha = 1.0f

                holder.binding.btnClaimReward.setOnClickListener {
                    onClaimClick(reward)
                }
            }
        }

    }

    override fun getItemCount(): Int = rewards.size

    fun updateUserData(newUserLevel: Int, newIsPremium: Boolean) {
        userLevel = newUserLevel
        isUserPremium = newIsPremium
        notifyDataSetChanged()
    }

    fun updateRewards(newRewards: List<BattlePassReward>) {
        rewards = newRewards
        notifyDataSetChanged()
    }
}

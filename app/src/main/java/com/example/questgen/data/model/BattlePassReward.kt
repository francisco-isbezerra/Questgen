package com.example.questgen.data.model

data class BattlePassReward(
    val id: Int,
    val level: Int,
    val isPremium: Boolean,
    val title: String,
    val description: String,
    var isClaimed: Boolean = false
)

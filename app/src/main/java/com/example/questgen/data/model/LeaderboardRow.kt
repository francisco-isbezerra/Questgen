package com.example.questgen.data.model

data class LeaderboardRow(
    val position: Int,
    val name: String,
    val game_coins: Int,
    val rank: String,
    val is_current_user: Boolean
)

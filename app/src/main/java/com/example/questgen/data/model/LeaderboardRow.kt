package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class LeaderboardRow(
    val position: Int,
    @SerializedName("nome") val name: String,
    @SerializedName("game_coins") val game_coins: Int,
    @SerializedName("patente") val rank: String,
    val is_current_user: Boolean,
    @SerializedName("imagem_url") val image_url: String? = null
)

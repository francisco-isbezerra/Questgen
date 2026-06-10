package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    @SerializedName("nome") val name: String,
    val email: String,
    @SerializedName("game_coins") val game_coins: Int,
    @SerializedName("patente") val rank: String
)

package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class Challenge(
    val id: Int,
    @SerializedName("jogo_id") val game_id: Int,
    @SerializedName("titulo") val title: String,
    @SerializedName("descricao") val description: String,
    @SerializedName("recompensa") val reward_amount: Int,
    @SerializedName("dificuldade") val difficulty_level: Int,
    @SerializedName("raridade") val rarity: String,            // "COMUM", "RARO", "LENDÁRIO"
    val status: String?,           // "AVAILABLE", "ACTIVE", "COMPLETED", "CLAIMED", "FALHOU", "DESISTIU"
    val tempo_restante_segundos: Long? // Calculado pelo servidor para o cronômetro reativo
)

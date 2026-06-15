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
    val status: String?,           // "AVAILABLE", "ACTIVE", "COMPLETED", "CLAIMED", "FALHOU", "DESISTIU", "PENDING_VALIDATION"
    val tempo_restante_segundos: Long?, // Calculado pelo servidor para o cronômetro reativo
    @SerializedName("tempo_total_segundos") val tempo_total_segundos: Long? = null,
    @SerializedName("comprovante_url") val comprovante_url: String? = null,
    @SerializedName("imagem_comprovante") val imagem_comprovante: String? = null
)

package com.example.questgen.data.model

data class Challenge(
    val id: Int,
    val game_id: Int,
    val title: String,
    val description: String,
    val reward_amount: Int,
    val difficulty_level: Int,
    val rarity: String,            // "COMUM", "RARO", "LENDÁRIO"
    val status: String?,           // "AVAILABLE", "ACTIVE", "COMPLETED", "CLAIMED", "FALHOU", "DESISTIU"
    val tempo_restante_segundos: Long? // Calculado pelo servidor para o cronômetro reativo
)

package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class ChallengeNotificationResponse(
    val status: String,
    val message: String?,
    @SerializedName("houve_mudanca") val houveMudanca: Boolean,
    val resultado: String?,
    @SerializedName("titulo_desafio") val tituloDesafio: String?,
    val recompensa: Int?
)

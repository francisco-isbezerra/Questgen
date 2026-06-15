package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class LeaderboardRow(
    val position: Int,
    @SerializedName("nome") val name: String,
    @SerializedName("game_coins") val game_coins: Int,
    @SerializedName("patente") val rank: String,
    val is_current_user: Boolean,
    @SerializedName("imagem_url") val image_url: String? = null,
    @SerializedName("moldura_neon") val moldura_neon: String? = null
) {
    fun getAbsoluteImageUrl(): String? {
        if (image_url.isNullOrEmpty()) return null
        return if (image_url.startsWith("http://") || image_url.startsWith("https://")) {
            image_url
        } else {
            "http://192.168.15.143/ApiQuestGen/$image_url"
        }
    }
}

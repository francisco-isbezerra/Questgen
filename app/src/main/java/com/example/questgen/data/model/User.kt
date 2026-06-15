package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    @SerializedName("nome") val name: String,
    val email: String,
    @SerializedName("game_coins") val game_coins: Int,
    @SerializedName("patente") val rank: String,
    @SerializedName("imagem_url") val image_url: String? = null,
    @SerializedName("xp_total") val xp_total: Int = 0,
    @SerializedName("nivel_atual") val nivel_atual: Int = 1,
    @SerializedName("is_premium") val is_premium: Boolean = false,
    @SerializedName("moldura_neon") val moldura_neon: String? = null,
    @SerializedName("clan_id") val clan_id: Int? = null,
    @SerializedName("descricao") val description: String? = null
) {
    fun getRankFromCoins(): String {
        return when {
            game_coins < 1000 -> "BRONZE"
            game_coins < 3000 -> "PRATA"
            game_coins < 10000 -> "OURO"
            game_coins < 30000 -> "PLATINA"
            game_coins < 100000 -> "DIAMANTE"
            else -> "LENDÁRIO"
        }
    }

    fun getAbsoluteImageUrl(): String? {
        if (image_url.isNullOrEmpty()) return null
        return if (image_url.startsWith("http://") || image_url.startsWith("https://")) {
            image_url
        } else {
            "http://192.168.15.143/ApiQuestGen/$image_url"
        }
    }
}

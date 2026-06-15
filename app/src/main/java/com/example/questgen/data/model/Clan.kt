package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class Clan(
    val id: Int,
    @SerializedName("nome") val name: String,
    val tag: String,
    @SerializedName("lider_id") val leader_id: Int,
    @SerializedName("logo_url") val logo_url: String?,
    @SerializedName("xp_total") val xp_total: Int
)

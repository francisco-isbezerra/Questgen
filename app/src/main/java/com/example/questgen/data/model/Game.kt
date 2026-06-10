package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class Game(
    val id: Int,
    @SerializedName("titulo") val title: String,
    @SerializedName("imagem_url") val image_url: String,
    @SerializedName("categoria") val category: String? = null
)

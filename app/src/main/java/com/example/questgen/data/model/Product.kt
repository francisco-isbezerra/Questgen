package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class Product(
    val id: Int,
    @SerializedName("nome") val name: String,
    @SerializedName("preco") val price: Int,
    @SerializedName("imagem_url") val image_url: String,
    @SerializedName("categoria") val category: String          // "Gift Cards", "Periféricos", "Skins", "Prêmios Digitais"
)

package com.example.questgen.data.model

import com.google.gson.annotations.SerializedName

data class Product(
    val id: Int,
    val name: String,
    val price: Int,
    @SerializedName("image_url") val image_url: String,
    val category: String,
    val description: String
)

package com.example.questgen.data.model

data class Product(
    val id: Int,
    val name: String,
    val price: Int,
    val image_url: String,
    val category: String          // "Gift Cards", "Periféricos", "Skins", "Prêmios Digitais"
)

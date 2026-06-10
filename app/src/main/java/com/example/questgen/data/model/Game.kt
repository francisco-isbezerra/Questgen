package com.example.questgen.data.model

data class Game(
    val id: Int,
    val title: String,
    val image_url: String,
    val category: String? = null
)

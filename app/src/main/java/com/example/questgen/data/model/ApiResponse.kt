package com.example.questgen.data.model

data class ApiResponse<T>(
    val status: String,       // "success" ou "error"
    val message: String?,     // Mensagem explicativa em caso de erro ou feedback
    val data: T?              // Dados genéricos de retorno da API
)

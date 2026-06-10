package com.example.questgen.data.model

data class ApiResponse<T>(
    val status: String,       // "success", "expired", ou "error"
    val message: String?,     // Mensagem explicativa
    val data: T?,             // Dados genéricos de retorno
    val updated_user: User? = null
)

package com.example.questgen.data.repository

import android.content.Context
import com.example.questgen.api.RetrofitClient
import com.example.questgen.data.model.ApiResponse
import com.example.questgen.data.model.Game

class GameRepository(context: Context) {

    private val apiService = RetrofitClient.getInstance(context)

    suspend fun getGames(): ApiResponse<List<Game>> {
        return apiService.obterJogos()
    }
}

package com.example.questgen.data.repository

import android.content.Context
import com.example.questgen.api.RetrofitClient
import com.example.questgen.data.model.ApiResponse
import com.example.questgen.data.model.Challenge
import com.example.questgen.data.model.User

class ChallengeRepository(context: Context) {

    private val apiService = RetrofitClient.getInstance(context)

    suspend fun generateRandomChallenge(userId: Int, gameId: Int): ApiResponse<Challenge> {
        return apiService.gerarDesafioAleatorio(userId, gameId)
    }

    suspend fun acceptChallenge(userId: Int, challengeId: Int): ApiResponse<Challenge> {
        val payload = mapOf("user_id" to userId, "challenge_id" to challengeId)
        return apiService.aceitarDesafio(payload)
    }

    suspend fun getActiveChallenge(userId: Int): ApiResponse<Challenge> {
        return apiService.obterDesafioAtivo(userId)
    }

    suspend fun claimReward(userId: Int, challengeId: Int): ApiResponse<User> {
        val payload = mapOf("user_id" to userId, "challenge_id" to challengeId)
        return apiService.reivindicarRecompensa(payload)
    }

    suspend fun giveUpChallenge(userId: Int, challengeId: Int): ApiResponse<User> {
        val payload = mapOf("user_id" to userId, "challenge_id" to challengeId)
        return apiService.desistirDesafio(payload)
    }

    suspend fun getRanking(userId: Int): ApiResponse<List<com.example.questgen.data.model.LeaderboardRow>> {
        return apiService.obterRankingGlobal(userId)
    }

    suspend fun getShopItems(): ApiResponse<List<com.example.questgen.data.model.Product>> {
        return apiService.obterItensLoja()
    }
}

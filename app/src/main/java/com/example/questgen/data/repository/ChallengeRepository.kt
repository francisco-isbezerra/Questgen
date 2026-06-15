package com.example.questgen.data.repository

import android.content.Context
import com.example.questgen.api.RetrofitClient
import com.example.questgen.data.model.ApiResponse
import com.example.questgen.data.model.Challenge
import com.example.questgen.data.model.Clan
import com.example.questgen.data.model.DashboardChallengesResponse
import com.example.questgen.data.model.LeaderboardRow
import com.example.questgen.data.model.Product
import com.example.questgen.data.model.User

import okhttp3.MultipartBody
import okhttp3.RequestBody

class ChallengeRepository(context: Context) {

    private val apiService = RetrofitClient.getInstance(context)

    suspend fun generateRandomChallenge(userId: Int, gameId: Int): ApiResponse<Challenge> {
        return apiService.gerarDesafioAleatorio(userId, gameId)
    }

    suspend fun acceptChallenge(userId: Int, challengeId: Int): ApiResponse<Challenge> {
        val payload = mapOf("user_id" to userId, "challenge_id" to challengeId)
        return apiService.aceitarDesafio(payload)
    }

    suspend fun getActiveChallenge(userId: Int): DashboardChallengesResponse {
        return apiService.obterDesafioAtivo(userId)
    }

    suspend fun claimReward(userId: Int, challengeId: Int, comprovanteUrl: String): ApiResponse<User> {
        val payload = mapOf("user_id" to userId, "challenge_id" to challengeId, "comprovante_url" to comprovanteUrl)
        return apiService.reivindicarRecompensa(payload)
    }

    suspend fun uploadComprovante(
        userId: RequestBody,
        challengeId: RequestBody,
        imagePart: MultipartBody.Part
    ): ApiResponse<Unit> {
        return apiService.uploadComprovante(userId, challengeId, imagePart)
    }

    suspend fun giveUpChallenge(userId: Int, challengeId: Int): ApiResponse<User> {
        val payload = mapOf("user_id" to userId, "challenge_id" to challengeId)
        return apiService.desistirDesafio(payload)
    }

    suspend fun getRanking(userId: Int): ApiResponse<List<LeaderboardRow>> {
        return apiService.obterRankingGlobal(userId)
    }

    suspend fun getShopItems(): ApiResponse<List<Product>> {
        return apiService.obterItensLoja()
    }

    suspend fun getChallengesByGame(gameId: Int): ApiResponse<List<Challenge>> {
        return apiService.obterDesafiosPorJogo(gameId)
    }

    suspend fun getRankingClans(): ApiResponse<List<Clan>> {
        return apiService.obterRankingClans()
    }

    suspend fun approveChallenge(userId: Int, challengeId: Int): ApiResponse<User> {
        return apiService.aprovarDesafio(mapOf("user_id" to userId, "challenge_id" to challengeId))
    }

    suspend fun getHistory(userId: Int): ApiResponse<List<Challenge>> {
        return apiService.obterHistorico(userId)
    }

    suspend fun verificarNotificacoes(userId: Int): com.example.questgen.data.model.ChallengeNotificationResponse {
        return apiService.verificarNotificacoes(userId)
    }
}

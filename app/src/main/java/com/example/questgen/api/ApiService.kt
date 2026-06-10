package com.example.questgen.api

import com.example.questgen.data.model.*
import retrofit2.http.*

interface ApiService {

    @POST("cadastro.php")
    suspend fun registrarUsuario(
        @Body campos: Map<String, String>
    ): ApiResponse<User>

    @POST("login.php")
    suspend fun efetuarLogin(
        @Body credenciais: Map<String, String>
    ): ApiResponse<User>

    @GET("buscar_jogos.php")
    suspend fun obterJogos(): ApiResponse<List<Game>>

    @GET("gerar_desafio.php")
    suspend fun gerarDesafioAleatorio(
        @Query("user_id") userId: Int,
        @Query("game_id") gameId: Int
    ): ApiResponse<Challenge>

    @POST("aceitar_desafio.php")
    suspend fun aceitarDesafio(
        @Body payload: Map<String, Int> // user_id, challenge_id
    ): ApiResponse<Challenge>

    @GET("buscar_desafio_ativo.php")
    suspend fun obterDesafioAtivo(
        @Query("user_id") userId: Int
    ): ApiResponse<Challenge>

    @POST("reivindicar_recompensa.php")
    suspend fun reivindicarRecompensa(
        @Body payload: Map<String, Int> // user_id, challenge_id
    ): ApiResponse<User> // Retorna usuário com saldo atualizado

    @POST("desistir_desafio.php")
    suspend fun desistirDesafio(
        @Body payload: Map<String, Int> // user_id, challenge_id
    ): ApiResponse<User> // Retorna usuário com penalidade deduzida

    @GET("buscar_loja.php")
    suspend fun obterItensLoja(): ApiResponse<List<Product>>

    @GET("buscar_ranking.php")
    suspend fun obterRankingGlobal(
        @Query("user_id") userId: Int
    ): ApiResponse<List<LeaderboardRow>>
}

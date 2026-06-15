package com.example.questgen.api

import com.example.questgen.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    ): DashboardChallengesResponse

    @POST("reivindicar_recompensa.php")
    suspend fun reivindicarRecompensa(
        @Body payload: Map<String, Any> // user_id, challenge_id, comprovante_url
    ): ApiResponse<User> // Retorna usuário com status pendente

    @Multipart
    @POST("upload_comprovante.php")
    suspend fun uploadComprovante(
        @Part("user_id") userId: RequestBody,
        @Part("challenge_id") challengeId: RequestBody,
        @Part image: MultipartBody.Part
    ): ApiResponse<Unit>

    @Multipart
    @POST("alterar_foto_perfil.php")
    suspend fun uploadFotoPerfil(
        @Part("user_id") userId: RequestBody,
        @Part image: MultipartBody.Part
    ): ApiResponse<String>

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

    @GET("buscar_desafios.php")
    suspend fun obterDesafiosPorJogo(
        @Query("game_id") gameId: Int
    ): ApiResponse<List<Challenge>>

    @POST("editar_perfil.php")
    suspend fun editarPerfil(
        @Body payload: Map<String, String>
    ): ApiResponse<User>

    @POST("excluir_conta.php")
    suspend fun excluirConta(
        @Body payload: Map<String, Int>
    ): ApiResponse<Map<String, String>>

    @POST("comprar_premium.php")
    suspend fun comprarPremium(
        @Body payload: Map<String, Int> // user_id
    ): ApiResponse<User>

    @POST("comprar_cosmetico.php")
    suspend fun comprarCosmetico(
        @Body payload: Map<String, Int> // user_id, product_id
    ): ApiResponse<User>

    @POST("criar_clan.php")
    suspend fun criarClan(
        @Body payload: Map<String, Any> // user_id, nome, tag
    ): ApiResponse<User>

    @GET("buscar_ranking_clans.php")
    suspend fun obterRankingClans(): ApiResponse<List<Clan>>

    @POST("aprovar_desafio.php")
    suspend fun aprovarDesafio(
        @Body payload: Map<String, Int> // user_id, challenge_id
    ): ApiResponse<User>

    @GET("buscar_historico.php")
    suspend fun obterHistorico(
        @Query("user_id") userId: Int
    ): ApiResponse<List<Challenge>>

    @GET("verificar_notificacoes.php")
    suspend fun verificarNotificacoes(
        @Query("user_id") userId: Int
    ): ChallengeNotificationResponse
}

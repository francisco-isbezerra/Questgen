package com.example.questgen.data.repository

import android.content.Context
import com.example.questgen.api.RetrofitClient
import com.example.questgen.data.model.ApiResponse
import com.example.questgen.data.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody

class UserRepository(private val context: Context) {

    private val apiService = RetrofitClient.getInstance(context)
    private val sharedPrefs = context.getSharedPreferences("QuestGenPrefs", Context.MODE_PRIVATE)

    suspend fun login(email: String, password: String): ApiResponse<User> {
        val credentials = mapOf("email" to email, "password" to password)
        val response = apiService.efetuarLogin(credentials)
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }

    suspend fun register(name: String, email: String, password: String): ApiResponse<User> {
        val fields = mapOf("name" to name, "email" to email, "password" to password)
        val response = apiService.registrarUsuario(fields)
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }

    fun getUserId(): Int {
        return sharedPrefs.getInt("USER_ID", -1)
    }

    fun getSavedUser(): User? {
        val id = sharedPrefs.getInt("USER_ID", -1)
        if (id == -1) return null
        val name = sharedPrefs.getString("USER_NAME", "") ?: ""
        val email = sharedPrefs.getString("USER_EMAIL", "") ?: ""
        val coins = sharedPrefs.getInt("USER_COINS", 0)
        val rank = sharedPrefs.getString("USER_RANK", "COMUM") ?: "COMUM"
        val imageUrl = sharedPrefs.getString("USER_IMAGE", null)
        val xp = sharedPrefs.getInt("USER_XP", 0)
        val level = sharedPrefs.getInt("USER_LEVEL", 1)
        val premium = sharedPrefs.getBoolean("USER_PREMIUM", false)
        val frame = sharedPrefs.getString("USER_FRAME", null)
        val clanId = sharedPrefs.getInt("USER_CLAN", -1).let { if (it == -1) null else it }
        val description = sharedPrefs.getString("USER_DESCRIPTION", null)
        return User(id, name, email, coins, rank, imageUrl, xp, level, premium, frame, clanId, description)
    }

    fun saveUserSession(user: User) {
        sharedPrefs.edit().apply {
            putInt("USER_ID", user.id)
            putString("USER_NAME", user.name)
            putString("USER_EMAIL", user.email)
            putInt("USER_COINS", user.game_coins)
            putString("USER_RANK", user.rank)
            putString("USER_IMAGE", user.image_url)
            putInt("USER_XP", user.xp_total)
            putInt("USER_LEVEL", user.nivel_atual)
            putBoolean("USER_PREMIUM", user.is_premium)
            putString("USER_FRAME", user.moldura_neon)
            putInt("USER_CLAN", user.clan_id ?: -1)
            putString("USER_DESCRIPTION", user.description)
            apply()
        }
    }

    fun clearSession() {
        sharedPrefs.edit().clear().apply()
    }

    suspend fun editarPerfil(userId: Int, name: String, imageUrl: String?, description: String?): ApiResponse<User> {
        val payload = mutableMapOf("user_id" to userId.toString(), "name" to name)
        if (imageUrl != null) {
            payload["image_url"] = imageUrl
        } else {
            payload["image_url"] = ""
        }
        if (description != null) {
            payload["description"] = description
        } else {
            payload["description"] = ""
        }
        val response = apiService.editarPerfil(payload)
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }

    suspend fun uploadFotoPerfil(
        userId: RequestBody,
        imagePart: MultipartBody.Part
    ): ApiResponse<String> {
        return apiService.uploadFotoPerfil(userId, imagePart)
    }

    suspend fun excluirConta(userId: Int): ApiResponse<Map<String, String>> {
        val payload = mapOf("user_id" to userId)
        val response = apiService.excluirConta(payload)
        if (response.status == "success") {
            clearSession()
        }
        return response
    }

    suspend fun comprarPremium(userId: Int): ApiResponse<User> {
        val response = apiService.comprarPremium(mapOf("user_id" to userId))
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }

    suspend fun comprarCosmetico(userId: Int, productId: Int): ApiResponse<User> {
        val response = apiService.comprarCosmetico(mapOf("user_id" to userId, "product_id" to productId))
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }

    suspend fun criarClan(userId: Int, nome: String, tag: String, description: String): ApiResponse<User> {
        val payload = mapOf("user_id" to userId, "nome" to nome, "tag" to tag, "description" to description)
        val response = apiService.criarClan(payload)
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }
}

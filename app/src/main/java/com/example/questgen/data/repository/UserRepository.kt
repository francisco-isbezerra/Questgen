package com.example.questgen.data.repository

import android.content.Context
import com.example.questgen.api.RetrofitClient
import com.example.questgen.data.model.ApiResponse
import com.example.questgen.data.model.User

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
        return User(id, name, email, coins, rank, imageUrl)
    }

    fun saveUserSession(user: User) {
        sharedPrefs.edit().apply {
            putInt("USER_ID", user.id)
            putString("USER_NAME", user.name)
            putString("USER_EMAIL", user.email)
            putInt("USER_COINS", user.game_coins)
            putString("USER_RANK", user.rank)
            putString("USER_IMAGE", user.image_url)
            apply()
        }
    }

    fun clearSession() {
        sharedPrefs.edit().clear().apply()
    }

    suspend fun editarPerfil(userId: Int, name: String, imageUrl: String?): ApiResponse<User> {
        val payload = mutableMapOf("user_id" to userId.toString(), "name" to name)
        if (imageUrl != null) {
            payload["image_url"] = imageUrl
        } else {
            payload["image_url"] = ""
        }
        val response = apiService.editarPerfil(payload)
        if (response.status == "success" && response.data != null) {
            saveUserSession(response.data)
        }
        return response
    }

    suspend fun excluirConta(userId: Int): ApiResponse<Map<String, String>> {
        val payload = mapOf("user_id" to userId)
        val response = apiService.excluirConta(payload)
        if (response.status == "success") {
            clearSession()
        }
        return response
    }
}

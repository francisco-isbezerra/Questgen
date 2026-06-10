package com.example.questgen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questgen.data.model.User
import com.example.questgen.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _sessionState = MutableStateFlow<User?>(null)
    val sessionState: StateFlow<User?> = _sessionState

    init {
        checkSession()
    }

    fun checkSession() {
        val user = userRepository.getSavedUser()
        _sessionState.value = user
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = userRepository.login(email, password)
                if (response.status == "success" && response.data != null) {
                    _authState.value = AuthState.Success(response.data)
                    _sessionState.value = response.data
                } else {
                    _authState.value = AuthState.Error(response.message ?: "Credenciais inválidas")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Falha de conexão com o servidor local: ${e.message}")
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = userRepository.register(name, email, password)
                if (response.status == "success" && response.data != null) {
                    _authState.value = AuthState.Success(response.data)
                    _sessionState.value = response.data
                } else {
                    _authState.value = AuthState.Error(response.message ?: "Erro ao criar conta")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Falha de conexão com o servidor local: ${e.message}")
            }
        }
    }

    fun logout() {
        userRepository.clearSession()
        _sessionState.value = null
        _authState.value = AuthState.Idle
    }
}

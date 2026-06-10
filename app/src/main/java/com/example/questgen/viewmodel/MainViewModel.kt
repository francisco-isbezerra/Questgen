package com.example.questgen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questgen.data.model.LeaderboardRow
import com.example.questgen.data.model.User
import com.example.questgen.data.repository.ChallengeRepository
import com.example.questgen.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RankingState {
    object Loading : RankingState()
    data class Success(val list: List<LeaderboardRow>) : RankingState()
    data class Error(val message: String) : RankingState()
}

sealed class EditProfileState {
    object Idle : EditProfileState()
    object Loading : EditProfileState()
    data class Success(val message: String) : EditProfileState()
    data class Error(val message: String) : EditProfileState()
}

sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    data class Success(val message: String) : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val challengeRepository = ChallengeRepository(application)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _editProfileState = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val editProfileState: StateFlow<EditProfileState> = _editProfileState

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState

    fun resetEditProfileState() {
        _editProfileState.value = EditProfileState.Idle
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }

    private val _rankingState = MutableStateFlow<RankingState>(RankingState.Loading)
    val rankingState: StateFlow<RankingState> = _rankingState

    // Mock/Local Challenge History since there's no remote history endpoint
    private val _challengeHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val challengeHistory: StateFlow<List<Pair<String, String>>> = _challengeHistory

    init {
        loadCurrentUser()
        loadMockHistory()
    }

    fun loadCurrentUser() {
        val user = userRepository.getSavedUser()
        _currentUser.value = user
        if (user != null) {
            fetchRanking()
        }
    }

    fun logout() {
        userRepository.clearSession()
        _currentUser.value = null
    }

    fun updateUser(user: User) {
        userRepository.saveUserSession(user)
        _currentUser.value = user
        // Refresh ranking as user stats changed
        fetchRanking()
    }

    fun fetchRanking() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _rankingState.value = RankingState.Loading
            try {
                val response = challengeRepository.getRanking(user.id)
                if (response.status == "success" && response.data != null) {
                    _rankingState.value = RankingState.Success(response.data)
                } else {
                    _rankingState.value = RankingState.Error(response.message ?: "Erro ao obter ranking")
                }
            } catch (e: Exception) {
                _rankingState.value = RankingState.Error("Sem conexão com o servidor local: ${e.message}")
            }
        }
    }

    private fun loadMockHistory() {
        _challengeHistory.value = listOf(
            "Neural Network Breach" to "+450 GC, Concluído há 3 horas",
            "Neon Kinetic Strike" to "+200 GC, Concluído há 1 dia",
            "Database Overrun" to "+300 GC, Concluído há 2 dias"
        )
    }

    fun addHistoryEntry(title: String, rewardMessage: String) {
        val currentList = _challengeHistory.value.toMutableList()
        currentList.add(0, title to rewardMessage)
        _challengeHistory.value = currentList
    }

    fun editarPerfil(name: String, imageUrl: String?) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _editProfileState.value = EditProfileState.Loading
            try {
                val response = userRepository.editarPerfil(user.id, name, imageUrl)
                if (response.status == "success" && response.data != null) {
                    _currentUser.value = response.data
                    _editProfileState.value = EditProfileState.Success(response.message ?: "Perfil atualizado!")
                    fetchRanking()
                } else {
                    _editProfileState.value = EditProfileState.Error(response.message ?: "Erro ao atualizar perfil")
                }
            } catch (e: Exception) {
                _editProfileState.value = EditProfileState.Error("Falha de conexão: ${e.message}")
            }
        }
    }

    fun excluirConta() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            try {
                val response = userRepository.excluirConta(user.id)
                if (response.status == "success") {
                    _deleteAccountState.value = DeleteAccountState.Success(response.message ?: "Conta excluída")
                    logout()
                } else {
                    _deleteAccountState.value = DeleteAccountState.Error(response.message ?: "Erro ao excluir conta")
                }
            } catch (e: Exception) {
                _deleteAccountState.value = DeleteAccountState.Error("Falha de conexão: ${e.message}")
            }
        }
    }
}

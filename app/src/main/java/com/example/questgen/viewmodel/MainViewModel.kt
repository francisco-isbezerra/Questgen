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

sealed class PremiumState {
    object Idle : PremiumState()
    object Loading : PremiumState()
    data class Success(val message: String, val user: User) : PremiumState()
    data class Error(val message: String) : PremiumState()
}

sealed class BuyCosmeticState {
    object Idle : BuyCosmeticState()
    object Loading : BuyCosmeticState()
    data class Success(val message: String, val user: User) : BuyCosmeticState()
    data class Error(val message: String) : BuyCosmeticState()
}

sealed class CreateClanState {
    object Idle : CreateClanState()
    object Loading : CreateClanState()
    data class Success(val message: String, val user: User) : CreateClanState()
    data class Error(val message: String) : CreateClanState()
}

sealed class ClanRankingState {
    object Loading : ClanRankingState()
    data class Success(val list: com.example.questgen.data.model.Clan) : ClanRankingState() // Wait, let's use List<Clan>
    data class SuccessList(val list: List<com.example.questgen.data.model.Clan>) : ClanRankingState()
    data class Error(val message: String) : ClanRankingState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val challengeRepository = ChallengeRepository(application)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _challengeNotificationEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val challengeNotificationEvent: kotlinx.coroutines.flow.SharedFlow<Unit> = _challengeNotificationEvent

    fun notifyChallengeStatusChanged() {
        _challengeNotificationEvent.tryEmit(Unit)
    }

    private val _editProfileState = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val editProfileState: StateFlow<EditProfileState> = _editProfileState

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState

    private val _premiumState = MutableStateFlow<PremiumState>(PremiumState.Idle)
    val premiumState: StateFlow<PremiumState> = _premiumState

    private val _buyCosmeticState = MutableStateFlow<BuyCosmeticState>(BuyCosmeticState.Idle)
    val buyCosmeticState: StateFlow<BuyCosmeticState> = _buyCosmeticState

    private val _createClanState = MutableStateFlow<CreateClanState>(CreateClanState.Idle)
    val createClanState: StateFlow<CreateClanState> = _createClanState

    private val _clanRankingState = MutableStateFlow<ClanRankingState>(ClanRankingState.Loading)
    val clanRankingState: StateFlow<ClanRankingState> = _clanRankingState

    fun resetEditProfileState() {
        _editProfileState.value = EditProfileState.Idle
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }

    fun resetPremiumState() {
        _premiumState.value = PremiumState.Idle
    }

    fun resetBuyCosmeticState() {
        _buyCosmeticState.value = BuyCosmeticState.Idle
    }

    fun resetCreateClanState() {
        _createClanState.value = CreateClanState.Idle
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
        fetchRanking(forceRefresh = true)
    }

    fun fetchRanking(forceRefresh: Boolean = false) {
        if (!forceRefresh && _rankingState.value is RankingState.Success) {
            return
        }
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

    fun editarPerfil(name: String, imageUrl: String?, description: String?) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _editProfileState.value = EditProfileState.Loading
            try {
                val response = userRepository.editarPerfil(user.id, name, imageUrl, description)
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

    fun comprarPremium() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _premiumState.value = PremiumState.Loading
            try {
                val response = userRepository.comprarPremium(user.id)
                if (response.status == "success" && response.data != null) {
                    _currentUser.value = response.data
                    _premiumState.value = PremiumState.Success(response.message ?: "Upgrade de Elite concluído!", response.data)
                } else {
                    _premiumState.value = PremiumState.Error(response.message ?: "Erro ao comprar Premium")
                }
            } catch (e: Exception) {
                _premiumState.value = PremiumState.Error("Falha de conexão: ${e.message}")
            }
        }
    }

    fun comprarCosmetico(productId: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _buyCosmeticState.value = BuyCosmeticState.Loading
            try {
                val response = userRepository.comprarCosmetico(user.id, productId)
                if (response.status == "success" && response.data != null) {
                    _currentUser.value = response.data
                    _buyCosmeticState.value = BuyCosmeticState.Success(response.message ?: "Cosmético ativado!", response.data)
                } else {
                    _buyCosmeticState.value = BuyCosmeticState.Error(response.message ?: "Erro ao comprar cosmético")
                }
            } catch (e: Exception) {
                _buyCosmeticState.value = BuyCosmeticState.Error("Falha de conexão: ${e.message}")
            }
        }
    }

    fun criarClan(nome: String, tag: String, description: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _createClanState.value = CreateClanState.Loading
            try {
                val response = userRepository.criarClan(user.id, nome, tag, description)
                if (response.status == "success" && response.data != null) {
                    _currentUser.value = response.data
                    _createClanState.value = CreateClanState.Success(response.message ?: "Clã criado com sucesso!", response.data)
                } else {
                    _createClanState.value = CreateClanState.Error(response.message ?: "Erro ao criar clã")
                }
            } catch (e: Exception) {
                _createClanState.value = CreateClanState.Error("Falha de conexão: ${e.message}")
            }
        }
    }

    fun obterRankingClans(forceRefresh: Boolean = false) {
        if (!forceRefresh && _clanRankingState.value is ClanRankingState.SuccessList) {
            return
        }
        viewModelScope.launch {
            _clanRankingState.value = ClanRankingState.Loading
            try {
                val response = challengeRepository.getRankingClans()
                if (response.status == "success" && response.data != null) {
                    _clanRankingState.value = ClanRankingState.SuccessList(response.data)
                } else {
                    _clanRankingState.value = ClanRankingState.Error(response.message ?: "Erro ao obter ranking de clãs")
                }
            } catch (e: Exception) {
                _clanRankingState.value = ClanRankingState.Error("Sem conexão com o servidor local: ${e.message}")
            }
        }
    }
}

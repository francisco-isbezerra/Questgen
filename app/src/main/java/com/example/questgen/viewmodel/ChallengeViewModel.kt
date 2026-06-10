package com.example.questgen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questgen.data.model.Challenge
import com.example.questgen.data.model.User
import com.example.questgen.data.repository.ChallengeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ActiveChallengeState {
    object Loading : ActiveChallengeState()
    data class Success(val challenge: Challenge?) : ActiveChallengeState()
    data class Error(val message: String) : ActiveChallengeState()
}

sealed class GenerateChallengeState {
    object Idle : GenerateChallengeState()
    object Loading : GenerateChallengeState()
    data class Success(val challenge: Challenge) : GenerateChallengeState()
    data class Error(val message: String) : GenerateChallengeState()
}

sealed class ChallengeActionState {
    object Idle : ChallengeActionState()
    object Loading : ChallengeActionState()
    data class Success(val message: String, val updatedUser: User? = null) : ChallengeActionState()
    data class Error(val message: String) : ChallengeActionState()
}

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val challengeRepository = ChallengeRepository(application)

    private val _activeChallengeState = MutableStateFlow<ActiveChallengeState>(ActiveChallengeState.Loading)
    val activeChallengeState: StateFlow<ActiveChallengeState> = _activeChallengeState

    private val _generateChallengeState = MutableStateFlow<GenerateChallengeState>(GenerateChallengeState.Idle)
    val generateChallengeState: StateFlow<GenerateChallengeState> = _generateChallengeState

    private val _actionState = MutableStateFlow<ChallengeActionState>(ChallengeActionState.Idle)
    val actionState: StateFlow<ChallengeActionState> = _actionState

    private val _timerString = MutableStateFlow("00:00:00")
    val timerString: StateFlow<String> = _timerString

    private var timerJob: Job? = null
    private var currentRemainingSeconds: Long = 0

    fun fetchActiveChallenge(userId: Int) {
        viewModelScope.launch {
            _activeChallengeState.value = ActiveChallengeState.Loading
            try {
                val response = challengeRepository.getActiveChallenge(userId)
                if (response.status == "success") {
                    val challenge = response.data
                    _activeChallengeState.value = ActiveChallengeState.Success(challenge)
                    if (challenge != null && challenge.status == "ACTIVE") {
                        val seconds = challenge.tempo_restante_segundos ?: 0
                        startCountdown(seconds)
                    } else {
                        stopCountdown()
                    }
                } else {
                    _activeChallengeState.value = ActiveChallengeState.Error(response.message ?: "Erro ao obter desafio ativo")
                }
            } catch (e: Exception) {
                _activeChallengeState.value = ActiveChallengeState.Error("Sem conexão com o servidor local: ${e.message}")
            }
        }
    }

    fun generateChallenge(userId: Int, gameId: Int) {
        viewModelScope.launch {
            _generateChallengeState.value = GenerateChallengeState.Loading
            try {
                val response = challengeRepository.generateRandomChallenge(userId, gameId)
                if (response.status == "success" && response.data != null) {
                    _generateChallengeState.value = GenerateChallengeState.Success(response.data)
                } else {
                    _generateChallengeState.value = GenerateChallengeState.Error(response.message ?: "Erro ao gerar desafio")
                }
            } catch (e: Exception) {
                _generateChallengeState.value = GenerateChallengeState.Error("Sem conexão com o servidor local: ${e.message}")
            }
        }
    }

    fun acceptChallenge(userId: Int, challengeId: Int) {
        viewModelScope.launch {
            _actionState.value = ChallengeActionState.Loading
            try {
                val response = challengeRepository.acceptChallenge(userId, challengeId)
                if (response.status == "success" && response.data != null) {
                    _actionState.value = ChallengeActionState.Success("Desafio aceito com sucesso!")
                    // Reload active challenge state
                    val challenge = response.data
                    _activeChallengeState.value = ActiveChallengeState.Success(challenge)
                    startCountdown(challenge.tempo_restante_segundos ?: 0)
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao aceitar desafio")
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Sem conexão: ${e.message}")
            }
        }
    }

    fun claimReward(userId: Int, challengeId: Int) {
        viewModelScope.launch {
            _actionState.value = ChallengeActionState.Loading
            try {
                val response = challengeRepository.claimReward(userId, challengeId)
                if (response.status == "success" && response.data != null) {
                    _actionState.value = ChallengeActionState.Success("Recompensa resgatada!", response.data)
                    _activeChallengeState.value = ActiveChallengeState.Success(null)
                    stopCountdown()
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao resgatar recompensa")
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Sem conexão: ${e.message}")
            }
        }
    }

    fun giveUpChallenge(userId: Int, challengeId: Int) {
        viewModelScope.launch {
            _actionState.value = ChallengeActionState.Loading
            try {
                val response = challengeRepository.giveUpChallenge(userId, challengeId)
                if (response.status == "success" && response.data != null) {
                    _actionState.value = ChallengeActionState.Success("Desafio abandonado.", response.data)
                    _activeChallengeState.value = ActiveChallengeState.Success(null)
                    stopCountdown()
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao desistir do desafio")
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Sem conexão: ${e.message}")
            }
        }
    }

    fun startCountdown(seconds: Long) {
        stopCountdown()
        currentRemainingSeconds = seconds
        timerJob = viewModelScope.launch {
            while (currentRemainingSeconds > 0) {
                _timerString.value = formatTime(currentRemainingSeconds)
                delay(1000)
                currentRemainingSeconds--
            }
            _timerString.value = "00:00:00"
        }
    }

    fun stopCountdown() {
        timerJob?.cancel()
        timerJob = null
    }

    fun resetActionState() {
        _actionState.value = ChallengeActionState.Idle
    }

    fun resetGenerateState() {
        _generateChallengeState.value = GenerateChallengeState.Idle
    }

    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
    }
}

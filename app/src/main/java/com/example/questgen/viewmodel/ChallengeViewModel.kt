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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed class ActiveChallengeState {
    object Loading : ActiveChallengeState()
    data class Success(val activeChallenge: Challenge?, val pendingChallenges: List<Challenge>) : ActiveChallengeState()
    data class Expired(val message: String, val updatedUser: User) : ActiveChallengeState()
    data class AutoCompleted(val message: String, val updatedUser: User) : ActiveChallengeState()
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

    private val _timerProgress = MutableStateFlow(100)
    val timerProgress: StateFlow<Int> = _timerProgress

    private var timerJob: Job? = null
    private var currentRemainingSeconds: Long = 0

    fun fetchActiveChallenge(userId: Int) {
        viewModelScope.launch {
            _activeChallengeState.value = ActiveChallengeState.Loading
            try {
                val response = challengeRepository.getActiveChallenge(userId)
                if (response.status == "success") {
                    val challenge = response.activeChallenge
                    val pendingList = response.pendingChallenges ?: emptyList()
                    _activeChallengeState.value = ActiveChallengeState.Success(challenge, pendingList)
                    if (challenge != null && challenge.status == "ACTIVE") {
                        val seconds = challenge.tempo_restante_segundos ?: 0
                        val total = challenge.tempo_total_segundos ?: seconds
                        startCountdown(seconds, total, userId)
                    } else {
                        stopCountdown()
                    }
                } else if (response.status == "expired") {
                    stopCountdown()
                    val user = response.updated_user
                    if (user != null) {
                        _activeChallengeState.value = ActiveChallengeState.Expired(response.message ?: "O tempo expirou!", user)
                    } else {
                        _activeChallengeState.value = ActiveChallengeState.Success(null, emptyList())
                    }
                } else if (response.status == "completed") {
                    stopCountdown()
                    val user = response.updated_user
                    if (user != null) {
                        _activeChallengeState.value = ActiveChallengeState.AutoCompleted(response.message ?: "Desafio concluído automaticamente!", user)
                    } else {
                        _activeChallengeState.value = ActiveChallengeState.Success(null, emptyList())
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
                    _activeChallengeState.value = ActiveChallengeState.Success(challenge, emptyList())
                    val seconds = challenge.tempo_restante_segundos ?: 0
                    val total = challenge.tempo_total_segundos ?: seconds
                    startCountdown(seconds, total, userId)
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao aceitar desafio")
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Sem conexão: ${e.message}")
            }
        }
    }

    fun claimReward(userId: Int, challengeId: Int, comprovanteUrl: String) {
        viewModelScope.launch {
            _actionState.value = ChallengeActionState.Loading
            try {
                val response = challengeRepository.claimReward(userId, challengeId, comprovanteUrl)
                if (response.status == "success" && response.data != null) {
                    _actionState.value = ChallengeActionState.Success("Comprovante enviado! Aguardando validação.", response.data)
                    _activeChallengeState.value = ActiveChallengeState.Success(
                        null,
                        emptyList()
                    )
                    // Trigger reload of active challenge to fetch from server with status PENDING_VALIDATION
                    fetchActiveChallenge(userId)
                    stopCountdown()
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao resgatar recompensa")
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Sem conexão: ${e.message}")
            }
        }
    }

    fun uploadComprovante(userId: Int, challengeId: Int, imageUri: android.net.Uri) {
        viewModelScope.launch {
            _actionState.value = ChallengeActionState.Loading
            try {
                val context = getApplication<Application>().applicationContext
                val contentResolver = context.contentResolver
                
                var fileName = "comprovante.png"
                val cursor = contentResolver.query(imageUri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }
                
                val tempFile = File(context.cacheDir, fileName)
                contentResolver.openInputStream(imageUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val mimeType = contentResolver.getType(imageUri) ?: "image/*"
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestFile = tempFile.asRequestBody(mediaType)
                val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                
                val userIdBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val challengeIdBody = challengeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                
                val response = challengeRepository.uploadComprovante(userIdBody, challengeIdBody, imagePart)
                if (response.status == "success") {
                    _actionState.value = ChallengeActionState.Success(response.message ?: "Comprovante enviado com sucesso!")
                    fetchActiveChallenge(userId)
                    stopCountdown()
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao enviar comprovante")
                }
                
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Falha ao enviar comprovante: ${e.message}")
            }
        }
    }

    fun approveChallenge(userId: Int, challengeId: Int) {
        viewModelScope.launch {
            _actionState.value = ChallengeActionState.Loading
            try {
                val response = challengeRepository.approveChallenge(userId, challengeId)
                if (response.status == "success" && response.data != null) {
                    _actionState.value = ChallengeActionState.Success(response.message ?: "Desafio validado e recompensas creditadas!", response.data)
                    _activeChallengeState.value = ActiveChallengeState.Success(null, emptyList())
                    stopCountdown()
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao validar desafio")
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
                    _activeChallengeState.value = ActiveChallengeState.Success(null, emptyList())
                    stopCountdown()
                } else {
                    _actionState.value = ChallengeActionState.Error(response.message ?: "Erro ao desistir do desafio")
                }
            } catch (e: Exception) {
                _actionState.value = ChallengeActionState.Error("Sem conexão: ${e.message}")
            }
        }
    }

    fun startCountdown(seconds: Long, totalSeconds: Long, userId: Int) {
        stopCountdown()
        currentRemainingSeconds = seconds
        val total = if (totalSeconds > 0) totalSeconds else seconds
        timerJob = viewModelScope.launch {
            while (currentRemainingSeconds > 0) {
                _timerString.value = formatTime(currentRemainingSeconds)
                val pct = if (total > 0) ((currentRemainingSeconds.toDouble() / total.toDouble()) * 100).toInt() else 100
                _timerProgress.value = pct.coerceIn(0, 100)
                delay(1000)
                currentRemainingSeconds--
            }
            _timerString.value = "00:00:00"
            _timerProgress.value = 0
            fetchActiveChallenge(userId)
        }
    }

    fun stopCountdown() {
        timerJob?.cancel()
        timerJob = null
    }

    fun resetActionState() {
        _actionState.value = ChallengeActionState.Idle
    }

    fun clearActiveChallengeState() {
        _activeChallengeState.value = ActiveChallengeState.Success(null, emptyList())
    }

    fun resetGenerateState() {
        _generateChallengeState.value = GenerateChallengeState.Idle
    }

    fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private val _gameChallenges = MutableStateFlow<List<Challenge>>(emptyList())
    val gameChallenges: StateFlow<List<Challenge>> = _gameChallenges

    fun fetchChallengesByGame(gameId: Int) {
        viewModelScope.launch {
            try {
                val response = challengeRepository.getChallengesByGame(gameId)
                if (response.status == "success" && response.data != null) {
                    _gameChallenges.value = response.data
                } else {
                    _gameChallenges.value = emptyList()
                }
            } catch (e: Exception) {
                _gameChallenges.value = emptyList()
            }
        }
    }

    fun getNextChallenges(currentChallenge: Challenge, gameChallengesList: List<Challenge>): List<Challenge> {
        val currentIndex = gameChallengesList.indexOfFirst {
            it.id == currentChallenge.id || it.title.equals(currentChallenge.title, ignoreCase = true)
        }
        if (currentIndex == -1 || gameChallengesList.size <= 1) {
            return emptyList()
        }

        val nextChallenges = mutableListOf<Challenge>()
        for (i in 1 until gameChallengesList.size) {
            val nextIndex = (currentIndex + i) % gameChallengesList.size
            nextChallenges.add(gameChallengesList[nextIndex])
        }
        return nextChallenges
    }

    private val _historyState = MutableStateFlow<List<Challenge>>(emptyList())
    val historyState: StateFlow<List<Challenge>> = _historyState

    fun fetchHistory(userId: Int) {
        viewModelScope.launch {
            try {
                val response = challengeRepository.getHistory(userId)
                if (response.status == "success" && response.data != null) {
                    _historyState.value = response.data
                } else {
                    _historyState.value = emptyList()
                }
            } catch (e: Exception) {
                _historyState.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
    }
}

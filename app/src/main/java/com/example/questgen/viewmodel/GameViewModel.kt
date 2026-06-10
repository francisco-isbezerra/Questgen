package com.example.questgen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questgen.data.model.Game
import com.example.questgen.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GamesState {
    object Loading : GamesState()
    data class Success(val list: List<Game>) : GamesState()
    data class Error(val message: String) : GamesState()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepository = GameRepository(application)

    private val _gamesState = MutableStateFlow<GamesState>(GamesState.Loading)
    val gamesState: StateFlow<GamesState> = _gamesState

    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame: StateFlow<Game?> = _selectedGame

    init {
        fetchGames()
    }

    fun fetchGames() {
        viewModelScope.launch {
            _gamesState.value = GamesState.Loading
            try {
                val response = gameRepository.getGames()
                if (response.status == "success" && response.data != null) {
                    _gamesState.value = GamesState.Success(response.data)
                } else {
                    _gamesState.value = GamesState.Error(response.message ?: "Erro ao buscar jogos")
                }
            } catch (e: Exception) {
                _gamesState.value = GamesState.Error("Sem conexão com o servidor local: ${e.message}")
            }
        }
    }

    fun selectGame(game: Game) {
        _selectedGame.value = game
    }
}

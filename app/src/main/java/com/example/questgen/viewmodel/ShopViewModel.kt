package com.example.questgen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questgen.data.model.Product
import com.example.questgen.data.repository.ChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ShopState {
    object Loading : ShopState()
    data class Success(val list: List<Product>) : ShopState()
    data class Error(val message: String) : ShopState()
}

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val challengeRepository = ChallengeRepository(application)

    private val _shopState = MutableStateFlow<ShopState>(ShopState.Loading)
    val shopState: StateFlow<ShopState> = _shopState

    private val _currentTab = MutableStateFlow("PERIFÉRICOS") // PERIFÉRICOS, SKINS, CUPONS
    val currentTab: StateFlow<String> = _currentTab

    init {
        fetchShopItems()
    }

    fun fetchShopItems() {
        viewModelScope.launch {
            _shopState.value = ShopState.Loading
            try {
                val response = challengeRepository.getShopItems()
                if (response.status == "success" && response.data != null) {
                    _shopState.value = ShopState.Success(response.data)
                } else {
                    _shopState.value = ShopState.Error(response.message ?: "Erro ao carregar itens da loja")
                }
            } catch (e: Exception) {
                _shopState.value = ShopState.Error("Sem conexão com o servidor local: ${e.message}")
            }
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }
}

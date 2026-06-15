package com.example.questgen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questgen.data.model.User
import com.example.questgen.data.model.ApiResponse
import com.example.questgen.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val message: String, val user: User) : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState

    fun resetState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    fun salvarAlteracoes(userId: Int, newName: String, imageUri: android.net.Uri?, currentImageUrl: String?, newDescription: String?) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading
            try {
                var finalImageUrl = currentImageUrl

                // 1. Se o usuário selecionou uma nova imagem local, faz o upload binário
                if (imageUri != null) {
                    val context = getApplication<Application>().applicationContext
                    val contentResolver = context.contentResolver

                    var fileName = "perfil_${System.currentTimeMillis()}.png"
                    val cursor = contentResolver.query(imageUri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                val rawName = it.getString(nameIndex)
                                if (!rawName.isNullOrBlank()) fileName = rawName
                            }
                        }
                    }

                    val tempFile = File(context.cacheDir, fileName)
                    contentResolver.openInputStream(imageUri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                    val mediaType = mimeType.toMediaTypeOrNull()
                    val requestFile = tempFile.asRequestBody(mediaType)
                    val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                    val userIdBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    val uploadResponse = userRepository.uploadFotoPerfil(userIdBody, imagePart)

                    if (tempFile.exists()) tempFile.delete()

                    if (uploadResponse.status == "success") {
                        // "data" é o campo correto — mas verificamos também para robustez
                        if (!uploadResponse.data.isNullOrBlank()) {
                            finalImageUrl = uploadResponse.data
                        } else {
                            // Fallback: se o servidor retornou success mas data veio null por algum motivo,
                            // continua com a imagem atual para não apagar o perfil
                            android.util.Log.w("ProfileViewModel", "Upload success mas data nulo — mantendo imagem atual")
                        }
                    } else {
                        val errMsg = uploadResponse.message ?: "Erro ao fazer upload da foto de perfil"
                        android.util.Log.e("ProfileViewModel", "Upload falhou: $errMsg")
                        _updateState.value = ProfileUpdateState.Error(errMsg)
                        return@launch
                    }
                }

                // 2. Atualiza o perfil no banco de dados (nome + imagem + descrição) e persiste sessão local
                val editResponse = userRepository.editarPerfil(userId, newName, finalImageUrl, newDescription)
                if (editResponse.status == "success" && editResponse.data != null) {
                    _updateState.value = ProfileUpdateState.Success(
                        editResponse.message ?: "Perfil atualizado com sucesso!",
                        editResponse.data
                    )
                } else {
                    val errMsg = editResponse.message ?: "Erro ao atualizar dados do perfil"
                    android.util.Log.e("ProfileViewModel", "editarPerfil falhou: $errMsg")
                    _updateState.value = ProfileUpdateState.Error(errMsg)
                }
            } catch (e: Exception) {
                _updateState.value = ProfileUpdateState.Error("Falha na operação: ${e.message}")
            }
        }
    }
}

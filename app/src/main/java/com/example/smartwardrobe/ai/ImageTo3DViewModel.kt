package com.example.smartwardrobe.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImageTo3DUiState(
    val selectedImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val result: AiRenderResponse? = null,
    val errorMessage: String? = null
)

class ImageTo3DViewModel(
    private val repository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageTo3DUiState())
    val uiState: StateFlow<ImageTo3DUiState> = _uiState

    private var currentJob: Job? = null

    fun onImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            result = null,
            errorMessage = null
        )
    }

    fun generate3D() {
        val uri = _uiState.value.selectedImageUri ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select an image first."
            )
            return
        }

        currentJob?.cancel()

        currentJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    errorMessage = null
                )

                val response = repository.renderClothingImage(uri)

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    result = response
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to generate 3D preview."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

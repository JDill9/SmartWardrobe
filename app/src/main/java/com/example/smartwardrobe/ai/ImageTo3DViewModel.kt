package com.example.smartwardrobe.ai

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwardrobe.data.repository.ModelCacheRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImageTo3DUiState(
    val selectedImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val result: AiRenderResponse? = null,
    val errorMessage: String? = null,
    val cachedModelPath: String? = null
)

class ImageTo3DViewModel(
    private val repository: AiRepository,
    private val cacheRepository: ModelCacheRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ImageTo3DViewModel"
    }

    private val _uiState = MutableStateFlow(ImageTo3DUiState())
    val uiState: StateFlow<ImageTo3DUiState> = _uiState

    private var currentJob: Job? = null

    fun onImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            result = null,
            errorMessage = null,
            cachedModelPath = null
        )
    }

    /**
     * SIMPLE VERSION (for demo): call Tripo and send the REMOTE GLB URL
     * straight to the 3D viewer. No caching involved here.
     */
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
                    errorMessage = null,
                    result = null,
                    cachedModelPath = null
                )

                Log.d(TAG, "Sending image to AI...")
                val response = repository.renderClothingImage(uri)
                Log.d(TAG, "Got AI response: $response")

                // IMPORTANT: response.models[*].modelUrl is the HTTPS GLB from Tripo.
                // We keep that as-is so the WebView / <model-viewer> loads it directly.
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    result = response,
                    cachedModelPath = null
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate 3D model", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to generate 3D model: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

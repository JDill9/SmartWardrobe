package com.example.smartwardrobe.ui.additem

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.repository.LocalStorageRepository
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddItemUiState(
    val name: String = "",
    val category: ClothingCategory = ClothingCategory.TOP,
    val color: String = "",
    val brand: String = "",
    val size: String = "",
    val tags: String = "", // Comma-separated tags
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

class AddItemViewModel(
    private val context: Context,
    private val repository: WardrobeRepository = WardrobeRepository(),
    private val localStorage: LocalStorageRepository = LocalStorageRepository(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onCategoryChange(category: ClothingCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onColorChange(color: String) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    fun onBrandChange(brand: String) {
        _uiState.value = _uiState.value.copy(brand = brand)
    }

    fun onSizeChange(size: String) {
        _uiState.value = _uiState.value.copy(size = size)
    }

    fun onTagsChange(tags: String) {
        _uiState.value = _uiState.value.copy(tags = tags)
    }

    fun onImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveItem() {
        val state = _uiState.value

        // Validation
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a name")
            return
        }

        if (state.selectedImageUri == null) {
            _uiState.value = state.copy(errorMessage = "Please select an image")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // 1. Save image locally
            val imageResult = localStorage.saveImage(state.selectedImageUri)
            if (imageResult is Result.Error) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save image: ${imageResult.exception.message}"
                )
                return@launch
            }

            val imagePath = (imageResult as Result.Success).data

            // 2. Parse tags from comma-separated string
            val tagsList = state.tags
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // 3. Create item with local image path
            val item = WardrobeItem(
                name = state.name.trim(),
                category = state.category,
                color = state.color.trim(),
                brand = state.brand.trim().ifEmpty { null },
                size = state.size.trim(),
                imageUrl = imagePath, // Local file path
                tags = tagsList
            )

            // 4. Save item to Firestore
            val result = repository.addWardrobeItem(item)

            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
                is Result.Error -> {
                    // Clean up saved image if Firestore save fails
                    localStorage.deleteImage(imagePath)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Failed to save item"
                    )
                }
                is Result.Loading -> {
                    // Already handling loading state
                }
            }
        }
    }
}
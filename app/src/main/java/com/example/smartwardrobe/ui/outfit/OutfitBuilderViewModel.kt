package com.example.smartwardrobe.ui.outfit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.Outfit
import com.example.smartwardrobe.data.model.Season
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.repository.OutfitRepository
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.util.Result
import com.example.smartwardrobe.ai.AiNetworkModule
import com.example.smartwardrobe.ai.AiRenderResponse
import com.example.smartwardrobe.ai.NetworkAiRepository
import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OutfitBuilderUiState(
    val wardrobeItems: Map<ClothingCategory, List<WardrobeItem>> = emptyMap(),
    val selectedItems: Map<ClothingCategory, WardrobeItem?> = emptyMap(),
    val outfitName: String = "",
    val selectedOccasions: List<String> = emptyList(),
    val selectedSeason: Season? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    // 3D Generation
    val isGenerating3D: Boolean = false,
    val generated3DModelUrl: String? = null,
    val generationError: String? = null
)

class OutfitBuilderViewModel(
    private val wardrobeRepository: WardrobeRepository = WardrobeRepository(),
    private val outfitRepository: OutfitRepository = OutfitRepository(),
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val aiRepository = NetworkAiRepository(AiNetworkModule.api, contentResolver)

    private val _uiState = MutableStateFlow(OutfitBuilderUiState())
    val uiState: StateFlow<OutfitBuilderUiState> = _uiState.asStateFlow()

    init {
        loadWardrobeItems()
    }

    private fun loadWardrobeItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = wardrobeRepository.getAllWardrobeItems()

            when (result) {
                is Result.Success -> {
                    // Group items by category
                    val groupedItems = result.data.groupBy { it.category }

                    // Initialize selected items map with null for each category
                    val selectedItems = ClothingCategory.entries.associateWith { null as WardrobeItem? }

                    _uiState.value = _uiState.value.copy(
                        wardrobeItems = groupedItems,
                        selectedItems = selectedItems,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun selectItem(category: ClothingCategory, item: WardrobeItem?) {
        val currentSelected = _uiState.value.selectedItems.toMutableMap()
        currentSelected[category] = item
        _uiState.value = _uiState.value.copy(selectedItems = currentSelected)
    }

    fun deselectItem(category: ClothingCategory) {
        selectItem(category, null)
    }

    fun setOutfitName(name: String) {
        _uiState.value = _uiState.value.copy(outfitName = name)
    }

    fun toggleOccasion(occasion: String) {
        val current = _uiState.value.selectedOccasions.toMutableList()
        if (current.contains(occasion)) {
            current.remove(occasion)
        } else {
            current.add(occasion)
        }
        _uiState.value = _uiState.value.copy(selectedOccasions = current)
    }

    fun setSeason(season: Season?) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun getSelectedItemsList(): List<WardrobeItem> {
        return _uiState.value.selectedItems.values.filterNotNull()
    }

    fun saveOutfit() {
        val state = _uiState.value
        val selectedItems = getSelectedItemsList()

        if (state.outfitName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter an outfit name")
            return
        }

        if (selectedItems.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Please select at least one item")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val outfit = Outfit(
                name = state.outfitName.trim(),
                itemIds = selectedItems.map { it.id },
                occasion = state.selectedOccasions,
                season = state.selectedSeason,
                isFavorite = false,
                imageUrl = selectedItems.firstOrNull()?.imageUrl ?: "" // Use first item's image as preview
            )

            val result = outfitRepository.createOutfit(outfit)

            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Failed to save outfit"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetBuilder() {
        val selectedItems = ClothingCategory.entries.associateWith { null as WardrobeItem? }
        _uiState.value = _uiState.value.copy(
            selectedItems = selectedItems,
            outfitName = "",
            selectedOccasions = emptyList(),
            selectedSeason = null,
            isSaved = false,
            errorMessage = null,
            generated3DModelUrl = null,
            generationError = null
        )
    }

    fun generate3DPreview() {
        val selectedItems = getSelectedItemsList()

        if (selectedItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                generationError = "Please select at least one item"
            )
            return
        }

        // Use the first selected item's image for 3D generation
        val firstItem = selectedItems.first()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating3D = true,
                generationError = null,
                generated3DModelUrl = null
            )

            try {
                val imageUri = Uri.parse("file://${firstItem.imageUrl}")
                val response = aiRepository.renderClothingImage(imageUri)

                val modelUrl = response.models.firstOrNull()?.modelUrl

                _uiState.value = _uiState.value.copy(
                    isGenerating3D = false,
                    generated3DModelUrl = modelUrl,
                    generationError = if (modelUrl == null) "No 3D model generated" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating3D = false,
                    generationError = e.message ?: "Failed to generate 3D preview"
                )
            }
        }
    }

    fun clear3DModel() {
        _uiState.value = _uiState.value.copy(
            generated3DModelUrl = null,
            generationError = null
        )
    }
}

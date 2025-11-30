package com.example.smartwardrobe.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwardrobe.data.model.Outfit
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.repository.OutfitRepository
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OutfitWithItems(
    val outfit: Outfit,
    val items: List<WardrobeItem>
)

data class SavedOutfitsUiState(
    val outfits: List<OutfitWithItems> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val viewing3DModelUrl: String? = null  // URL of 3D model currently being viewed
)

class SavedOutfitsViewModel(
    private val outfitRepository: OutfitRepository = OutfitRepository(),
    private val wardrobeRepository: WardrobeRepository = WardrobeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedOutfitsUiState())
    val uiState: StateFlow<SavedOutfitsUiState> = _uiState.asStateFlow()

    init {
        loadOutfits()
    }

    private fun loadOutfits() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Use Flow for real-time updates
            outfitRepository.getAllOutfitsFlow().collect { outfitsResult ->
                when (outfitsResult) {
                    is Result.Success -> {
                        // Get all wardrobe items to match with outfits
                        val itemsResult = wardrobeRepository.getAllWardrobeItems()

                        if (itemsResult is Result.Success) {
                            val itemsMap = itemsResult.data.associateBy { it.id }

                            val outfitsWithItems = outfitsResult.data.map { outfit ->
                                val items = outfit.itemIds.mapNotNull { itemId ->
                                    itemsMap[itemId]
                                }
                                OutfitWithItems(outfit, items)
                            }

                            _uiState.value = _uiState.value.copy(
                                outfits = outfitsWithItems,
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                outfits = outfitsResult.data.map { OutfitWithItems(it, emptyList()) },
                                isLoading = false
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = outfitsResult.exception.message
                        )
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    fun toggleFavorite(outfitId: String) {
        viewModelScope.launch {
            outfitRepository.toggleFavorite(outfitId)
            // No need to manually refresh - Flow will auto-update
        }
    }

    fun deleteOutfit(outfitId: String) {
        viewModelScope.launch {
            outfitRepository.deleteOutfit(outfitId)
            // No need to manually refresh - Flow will auto-update
        }
    }

    fun refresh() {
        // No longer needed - Flow handles real-time updates
        // Keeping method for backward compatibility but it's a no-op
    }

    fun view3DModel(modelUrl: String) {
        _uiState.value = _uiState.value.copy(viewing3DModelUrl = modelUrl)
    }

    fun close3DViewer() {
        _uiState.value = _uiState.value.copy(viewing3DModelUrl = null)
    }
}

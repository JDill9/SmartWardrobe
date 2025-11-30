package com.example.smartwardrobe.ui.outfit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwardrobe.ai.TripoAiRepository
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.Outfit
import com.example.smartwardrobe.data.model.Season
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.repository.ModelCacheRepository
import com.example.smartwardrobe.data.repository.OutfitRepository
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.util.ImageCompositor
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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
    private val contentResolver: ContentResolver,
    private val cacheRepository: ModelCacheRepository,
    applicationContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "OutfitBuilderViewModel"
    }

    private val appContext = applicationContext.applicationContext

    private val aiRepository = TripoAiRepository(
        contentResolver = contentResolver,
        tripoApiKey = "tsk_97_kK-zVy2bQzZBIkbeSuwsf3XD3Za6Sk3g8tPJZBd0"
    )

    private val _uiState = MutableStateFlow(OutfitBuilderUiState())
    val uiState: StateFlow<OutfitBuilderUiState> = _uiState.asStateFlow()

    init {
        loadWardrobeItems()

        // Cleanup old composite images
        viewModelScope.launch(Dispatchers.IO) {
            ImageCompositor.cleanupOldComposites(appContext)
        }
    }

    private fun loadWardrobeItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Use Flow for real-time updates
            wardrobeRepository.getAllWardrobeItemsFlow().collect { result ->
                when (result) {
                    is Result.Success -> {
                        // Group items by category
                        val groupedItems = result.data.groupBy { it.category }

                        // Initialize selected items map with null for each category if not already set
                        val selectedItems = if (_uiState.value.selectedItems.isEmpty()) {
                            ClothingCategory.entries.associateWith { null as WardrobeItem? }
                        } else {
                            _uiState.value.selectedItems
                        }

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
                imageUrl = selectedItems.firstOrNull()?.imageUrl ?: "", // Use first item's image as preview
                ai3DModelUrl = state.generated3DModelUrl // Save generated 3D model URL
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating3D = true,
                generationError = null,
                generated3DModelUrl = null
            )

            try {
                // Build map of non-null selected items
                val itemsByCategory = _uiState.value.selectedItems
                    .filterValues { it != null }
                    .mapValues { it.value!! }

                Log.d(TAG, "Compositing ${itemsByCategory.size} items into outfit image...")

                // Composite images on IO dispatcher
                val compositeUri = withContext(Dispatchers.IO) {
                    ImageCompositor.compositeOutfitImages(
                        context = appContext,
                        selectedItems = itemsByCategory
                    )
                }

                Log.d(TAG, "Composite created: $compositeUri")
                Log.d(TAG, "Generating 3D model from composite...")

                // Generate 3D model from composite
                val response = aiRepository.renderClothingImage(compositeUri)

                Log.d(TAG, "3D model generated successfully!")

                // Cache the model and get the ORIGINAL HTTPS URL
                val firstModel = response.models.firstOrNull()

                if (firstModel != null) {
                    val originalUrl = firstModel.modelUrl
                    Log.d(TAG, "Model URL: $originalUrl")
                    Log.d(TAG, "Downloading and caching model...")

                    try {
                        // Download and cache the model
                        val cacheResult = cacheRepository.cacheModel(
                            modelUrl = originalUrl,
                            modelId = firstModel.id
                        )

                        cacheResult.onSuccess { cachedPath ->
                            Log.d(TAG, "✓ Model cached successfully at: $cachedPath")
                            Log.d(TAG, "✓ Passing remote URL to viewer: $originalUrl")

                            // IMPORTANT: Pass the ORIGINAL HTTPS URL to the viewer
                            // WebView will intercept this and serve from cache if available
                            _uiState.value = _uiState.value.copy(
                                isGenerating3D = false,
                                generated3DModelUrl = originalUrl,  // Use remote HTTPS URL
                                generationError = null
                            )
                        }.onFailure { e ->
                            Log.e(TAG, "✗ Failed to cache model: ${e.message}", e)
                            // Fallback: try remote URL (will be fetched from network)
                            _uiState.value = _uiState.value.copy(
                                isGenerating3D = false,
                                generated3DModelUrl = originalUrl,
                                generationError = "Failed to cache model, using remote URL"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Exception while caching: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            isGenerating3D = false,
                            generated3DModelUrl = originalUrl,
                            generationError = "Failed to cache model, using remote URL"
                        )
                    }
                } else {
                    Log.w(TAG, "No model in response")
                    _uiState.value = _uiState.value.copy(
                        isGenerating3D = false,
                        generated3DModelUrl = null,
                        generationError = "No 3D model generated"
                    )
                }
            } catch (e: IOException) {
                // Image compositing error
                Log.e(TAG, "Failed to composite images", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating3D = false,
                    generationError = "Failed to composite images: ${e.message}"
                )
            } catch (e: Exception) {
                // AI generation error
                Log.e(TAG, "Failed to generate 3D model", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating3D = false,
                    generationError = "Failed to generate 3D model: ${e.message}"
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

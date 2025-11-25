package com.example.smartwardrobe.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smartwardrobe.data.repository.ModelCacheRepository

class ImageTo3DViewModelFactory(
    private val repository: AiRepository,
    private val cacheRepository: ModelCacheRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageTo3DViewModel::class.java)) {
            return ImageTo3DViewModel(repository, cacheRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

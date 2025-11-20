package com.example.smartwardrobe.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImageTo3DViewModelFactory(
    private val repository: AiRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageTo3DViewModel::class.java)) {
            return ImageTo3DViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

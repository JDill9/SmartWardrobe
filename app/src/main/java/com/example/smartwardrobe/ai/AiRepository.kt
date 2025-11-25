package com.example.smartwardrobe.ai

import android.net.Uri

interface AiRepository {
    suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse

    /**
     * Load an already-generated Tripo model by taskId.
     * This does NOT create a new task or re-generate the model.
     */
    suspend fun loadExistingModel(taskId: String): AiRenderResponse
}

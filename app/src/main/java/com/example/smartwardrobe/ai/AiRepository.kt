package com.example.smartwardrobe.ai

import android.net.Uri

interface AiRepository {
    suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse
}

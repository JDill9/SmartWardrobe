package com.example.smartwardrobe.ai

import android.net.Uri
import kotlinx.coroutines.delay

class MockAiRepository : AiRepository {

    override suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse {
        // Simulate network / AI processing time
        delay(1500L)

        // Return fake preview data — good enough to demo the flow
        val mockModel = RenderedModel(
            id = "mock-model-1",
            previewImageUrl = "https://via.placeholder.com/512x512.png?text=Mock+3D+Preview",
            modelUrl = "https://example.com/models/mock_outfit.glb"
        )

        return AiRenderResponse(
            models = listOf(mockModel)
        )
    }
}

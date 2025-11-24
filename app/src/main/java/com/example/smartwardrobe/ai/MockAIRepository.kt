package com.example.smartwardrobe.ai

import android.net.Uri
import kotlinx.coroutines.delay

class MockAiRepository : AiRepository {

    override suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse {
        // Simulate AI processing time (2-3 seconds for realism)
        delay(2500L)

        // Sample clothing/fashion GLB models for demo
        val sampleModels = listOf(
            "https://raw.githubusercontent.com/AltspaceVR/universal-humanoid-avatar/main/examples/models/female-business.glb",
            "https://raw.githubusercontent.com/AltspaceVR/universal-humanoid-avatar/main/examples/models/male-casual.glb",
            "https://models.readyplayer.me/64bfa15f0e72c63d7c3934e6.glb"
        )

        val mockModel = RenderedModel(
            id = "mock-model-${System.currentTimeMillis()}",
            previewImageUrl = "",
            modelUrl = sampleModels.random()
        )

        return AiRenderResponse(
            models = listOf(mockModel)
        )
    }
}

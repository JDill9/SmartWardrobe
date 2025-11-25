package com.example.smartwardrobe.ai

import android.net.Uri
import kotlinx.coroutines.delay

/**
 * Simple mock AI repository used for testing the UI
 * without calling any real external API or using credits.
 */
class MockAiRepository : AiRepository {

    /**
     * Pretend to generate a 3D model from an uploaded image.
     */
    override suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse {
        // Simulate ~2.5 seconds of "AI processing"
        delay(2500L)

        val mockModel = RenderedModel(
            id = "mock-${System.currentTimeMillis()}",
            previewImageUrl = "",
            modelUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/ToyCar/glTF/ToyCar.gltf"
        )

        return AiRenderResponse(
            models = listOf(mockModel)
        )
    }

    /**
     * Mock version of loading an existing model by task ID.
     * Used for testing "load previous render" without hitting real API.
     */
    override suspend fun loadExistingModel(taskId: String): AiRenderResponse {
        delay(500L)

        val mockModel = RenderedModel(
            id = taskId,
            previewImageUrl = "",
            modelUrl = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/CesiumMan/glTF/CesiumMan.gltf"
        )

        return AiRenderResponse(
            models = listOf(mockModel)
        )
    }
}

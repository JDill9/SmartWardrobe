package com.example.smartwardrobe.ai

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class NetworkAiRepository(
    private val api: AiApi,
    private val contentResolver: ContentResolver
) : AiRepository {

    companion object {
        // TripoSR - fast single image to 3D (~10 seconds)
        private const val MODEL_VERSION = "e0d3fe8abce3ba86497ea3530d9eae59af7b2231b6c82bedfc32b0732d35ec3a"
        private const val POLL_INTERVAL_MS = 1000L
        private const val MAX_POLL_ATTEMPTS = 60  // 1 minute max
    }

    override suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse {
        return withContext(Dispatchers.IO) {
            // Convert image to base64 data URI
            val base64Image = convertToBase64DataUri(imageUri)

            // Create prediction request
            val request = ReplicatePredictionRequest(
                version = MODEL_VERSION,
                input = ReplicateInput(image = base64Image)
            )

            // Submit prediction
            val prediction = api.createPrediction(request)

            // Poll for completion
            val result = pollForCompletion(prediction.id)

            // Convert to AiRenderResponse
            convertToAiRenderResponse(result)
        }
    }

    private fun convertToBase64DataUri(imageUri: Uri): String {
        // Copy URI to temp file first (handles content:// URIs)
        val tempFile = File.createTempFile("smartwardrobe_upload_", ".jpg")

        contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        // Read file and convert to base64
        val bytes = tempFile.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        // Clean up temp file
        tempFile.delete()

        // Return as data URI
        return "data:image/jpeg;base64,$base64"
    }

    private suspend fun pollForCompletion(predictionId: String): ReplicatePredictionResponse {
        var attempts = 0

        while (attempts < MAX_POLL_ATTEMPTS) {
            val prediction = api.getPrediction(predictionId)

            when (prediction.status) {
                "succeeded" -> return prediction
                "failed", "canceled" -> {
                    throw Exception(prediction.error ?: "Prediction failed")
                }
                else -> {
                    // Still processing, wait and retry
                    delay(POLL_INTERVAL_MS)
                    attempts++
                }
            }
        }

        throw Exception("Prediction timed out after ${MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000} seconds")
    }

    private fun convertToAiRenderResponse(prediction: ReplicatePredictionResponse): AiRenderResponse {
        val outputs = prediction.output ?: throw Exception("No output from prediction")

        // Find the GLB file
        val glbUrl = outputs.find { it.endsWith(".glb") || it.endsWith(".obj") }
            ?: outputs.firstOrNull()
            ?: throw Exception("No model URL in output")

        val model = RenderedModel(
            id = UUID.randomUUID().toString(),
            previewImageUrl = outputs.find { it.endsWith(".png") || it.endsWith(".jpg") } ?: "",
            modelUrl = glbUrl
        )

        return AiRenderResponse(models = listOf(model))
    }
}

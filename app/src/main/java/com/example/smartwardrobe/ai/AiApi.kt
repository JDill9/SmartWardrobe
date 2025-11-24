package com.example.smartwardrobe.ai

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AiApi {

    @POST("predictions")
    suspend fun createPrediction(
        @Body request: ReplicatePredictionRequest
    ): ReplicatePredictionResponse

    @GET("predictions/{id}")
    suspend fun getPrediction(
        @Path("id") predictionId: String
    ): ReplicatePredictionResponse
}

// Request model for creating a prediction
data class ReplicatePredictionRequest(
    val version: String,
    val input: ReplicateInput
)

data class ReplicateInput(
    val image: String  // Base64 data URI or URL
)

// Response model for predictions
data class ReplicatePredictionResponse(
    val id: String,
    val status: String,  // "starting", "processing", "succeeded", "failed", "canceled"
    val output: List<String>? = null,  // URLs to output files (GLB)
    val error: String? = null,
    val urls: ReplicateUrls? = null
)

data class ReplicateUrls(
    val get: String? = null,
    val cancel: String? = null
)

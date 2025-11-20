package com.example.smartwardrobe.ai

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AiApi {

    @Multipart
    @POST("render3d")
    suspend fun render3D(
        @Part image: MultipartBody.Part
    ): AiRenderResponse
}

package com.example.smartwardrobe.ai

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class NetworkAiRepository(
    private val api: AiApi,
    private val contentResolver: ContentResolver
) : AiRepository {

    override suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse {
        // Perform IO operations off the main thread
        return withContext(Dispatchers.IO) {

            // Copy the selected image to a temporary file
            val tempFile = File.createTempFile("smartwardrobe_upload_", ".jpg")

            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Prepare multipart body for upload
            val requestBody = tempFile
                .asRequestBody("image/*".toMediaTypeOrNull())

            val multipartPart = MultipartBody.Part.createFormData(
                name = "image",
                filename = tempFile.name,
                body = requestBody
            )

            // Call the actual AI endpoint via Retrofit
            api.render3D(multipartPart)
        }
    }
}

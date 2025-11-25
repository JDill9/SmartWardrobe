package com.example.smartwardrobe.ai

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Tripo AI Official API integration.
 *
 * Flow:
 * 1) POST /upload        → get file_token (or image_token)
 * 2) POST /task          → create image_to_model task with that token
 * 3) GET  /task/{id}     → poll until status == "success"
 * 4) output.model_mesh.url      → GLB
 *    output.rendered_image.url  → preview image
 */
class TripoAiRepository(
    private val contentResolver: ContentResolver,
    private val tripoApiKey: String
) : AiRepository {

    companion object {
        private const val TAG = "TripoAI"
        private const val BASE_URL = "https://api.tripo3d.ai/v2/openapi"
        private const val TIMEOUT_SECONDS = 30L
        private const val POLL_INTERVAL_MS = 1_000L   // 1 second
        private const val MAX_POLL_ATTEMPTS = 60      // ~60 seconds
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun renderClothingImage(imageUri: Uri): AiRenderResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting Tripo AI generation for URI: $imageUri")

                // 1) Upload image → file_token / image_token
                val fileToken = uploadImage(imageUri)
                Log.d(TAG, "Image uploaded. token = $fileToken")

                // 2) Create image_to_model task
                val taskId = submitImageToModelTask(fileToken)
                Log.d(TAG, "image_to_model task created: $taskId")

                // 3) Poll until status == success
                val output = pollForCompletion(taskId)
                Log.d(TAG, "Task $taskId completed. Output JSON: $output")

                // 4) Extract URLs
                val glbUrl = extractGlbUrl(output)
                val previewUrl = extractPreviewUrl(output)

                Log.d(TAG, "Extracted GLB URL: $glbUrl")
                Log.d(TAG, "Extracted preview URL: $previewUrl")

                val model = RenderedModel(
                    id = taskId,
                    previewImageUrl = previewUrl,
                    modelUrl = glbUrl
                )

                AiRenderResponse(models = listOf(model))
            } catch (e: Exception) {
                Log.e(TAG, "Error generating 3D model", e)
                throw Exception("Failed to generate 3D model: ${e.message}", e)
            }
        }
    }

    /**
     * Load an already-generated model by taskId (no new generation).
     */
    override suspend fun loadExistingModel(taskId: String): AiRenderResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading existing Tripo task: $taskId")

                // Reuse the same polling logic – this just calls GET /task/{taskId}
                val output = pollForCompletion(taskId)

                val glbUrl = extractGlbUrl(output)
                val previewUrl = extractPreviewUrl(output)

                Log.d(TAG, "Extracted GLB URL (existing task): $glbUrl")
                Log.d(TAG, "Extracted preview URL (existing task): $previewUrl")

                val model = RenderedModel(
                    id = taskId,
                    previewImageUrl = previewUrl,
                    modelUrl = glbUrl
                )

                AiRenderResponse(models = listOf(model))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading existing Tripo task $taskId", e)
                throw Exception("Failed to load existing model: ${e.message}", e)
            }
        }
    }

    /**
     * Step 1: Upload image and get a token.
     *
     * Some accounts / API versions may use "file_token",
     * others may return "image_token". We support both.
     */
    private fun uploadImage(imageUri: Uri): String {
        val tempFile = uriToTempFile(imageUri)

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image.jpg",
                    RequestBody.create(
                        "image/jpeg".toMediaTypeOrNull(),
                        tempFile
                    )
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/upload")
                .header("Authorization", "Bearer $tripoApiKey")
                .post(requestBody)
                .build()

            Log.d(TAG, "Uploading image to Tripo /upload ...")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                Log.d(TAG, "Upload response code: ${response.code}")
                Log.d(TAG, "Upload response body: $responseBody")

                if (!response.isSuccessful) {
                    throw Exception("Upload error: ${response.code} - $responseBody")
                }

                val json = JSONObject(responseBody)

                // Some responses are shaped as { "data": { ... } }, others may be flat.
                val dataObj = json.optJSONObject("data") ?: json

                // Try both possible fields: file_token and image_token
                val fileToken = when {
                    dataObj.has("file_token") -> dataObj.optString("file_token")
                    dataObj.has("image_token") -> dataObj.optString("image_token")
                    else -> ""
                }.orEmpty()

                if (fileToken.isBlank()) {
                    // We log the full body so you can see the exact JSON in Logcat
                    throw Exception(
                        "Upload succeeded but no file_token or image_token was found. " +
                                "Response body: $responseBody"
                    )
                }

                return fileToken
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Step 2: Create an image_to_model task using the token.
     */
    private fun submitImageToModelTask(fileToken: String): String {
        val jsonBody = JSONObject().apply {
            put("type", "image_to_model")
            put("file", JSONObject().apply {
                put("type", "jpg")
                put("file_token", fileToken)
            })
            // Adjust model_version if Tripo updates it; this is a known working one.
            put("model_version", "v1.4-20240625")
            // Optional tuning:
            // put("texture", true)
            // put("pbr", true)
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/task")
            .header("Authorization", "Bearer $tripoApiKey")
            .post(requestBody)
            .build()

        Log.d(TAG, "Submitting 3D generation task to /task ...")
        Log.d(TAG, "Task request body: $jsonBody")

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            Log.d(TAG, "Task response code: ${response.code}")
            Log.d(TAG, "Task response body: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("API error: ${response.code} - $responseBody")
            }

            val json = JSONObject(responseBody)
            val data = json.optJSONObject("data") ?: json
            val taskId = data.optString("task_id")

            if (taskId.isBlank()) {
                throw Exception("Task created but task_id is blank. Body: $responseBody")
            }

            return taskId
        }
    }

    /**
     * Convert content Uri → temp File so OkHttp can upload it.
     */
    private fun uriToTempFile(imageUri: Uri): File {
        val tempFile = File.createTempFile("tripo_", ".jpg")
        contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    /**
     * Step 3: Poll /task/{taskId} until status == success or failed/timeout.
     *
     * Returns the "output" JSON object, which should include:
     *   - model_mesh: { url: "https://....glb", ... }
     *   - rendered_image: { url: "https://....webp", ... }
     */
    private suspend fun pollForCompletion(taskId: String): JSONObject {
        var attempts = 0

        while (attempts < MAX_POLL_ATTEMPTS) {
            val request = Request.Builder()
                .url("$BASE_URL/task/$taskId")
                .header("Authorization", "Bearer $tripoApiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Poll error: ${response.code} - $responseBody")
                    throw Exception("Poll error: ${response.code}")
                }

                val json = JSONObject(responseBody)
                val data = json.optJSONObject("data") ?: json
                val status = data.optString("status")

                Log.d(TAG, "Task $taskId status: $status (attempt $attempts)")
                Log.d(TAG, "Task poll body: $responseBody")

                when (status) {
                    "success" -> {
                        val output = data.getJSONObject("output")
                        Log.d(TAG, "Task $taskId SUCCESS, output: $output")
                        return output
                    }
                    "failed" -> {
                        val errorMsg = data.optString("error", "Unknown error")
                        throw Exception("Generation failed: $errorMsg")
                    }
                    "running", "queued" -> {
                        delay(POLL_INTERVAL_MS)
                        attempts++
                    }
                    else -> {
                        // status empty or unknown - wait a bit and retry
                        delay(POLL_INTERVAL_MS)
                        attempts++
                    }
                }
            }
        }

        throw Exception("Task timed out after ${(MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS) / 1000} seconds")
    }

    /**
     * Step 4a: Extract GLB URL from output.
     *
     * Expected shape:
     * {
     *   "model_mesh": { "url": "https://....glb", ... },
     *   "rendered_image": { "url": "https://....webp", ... }
     * }
     */
    private fun extractGlbUrl(output: JSONObject): String {
        val modelMesh = output.optJSONObject("model_mesh")
        val meshUrl = modelMesh?.optString("url").orEmpty()

        if (meshUrl.isNotBlank()) {
            return meshUrl
        }

        // Fallbacks in case Tripo changes fields
        val directModel = output.optString("model")
        if (directModel.isNotBlank()) return directModel

        val glbUrl = output.optString("glb_url")
        if (glbUrl.isNotBlank()) return glbUrl

        throw Exception("No GLB URL in output JSON: $output")
    }

    /**
     * Step 4b: Extract preview image URL from output (optional).
     */
    private fun extractPreviewUrl(output: JSONObject): String {
        val rendered = output.optJSONObject("rendered_image")
        val preview = rendered?.optString("url").orEmpty()

        if (preview.isNotBlank()) {
            return preview
        }

        // Fallbacks, just in case
        val directPreview = output.optString("preview_image")
        if (directPreview.isNotBlank()) return directPreview

        return ""
    }
}

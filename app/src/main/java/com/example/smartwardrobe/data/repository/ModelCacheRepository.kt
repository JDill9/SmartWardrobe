package com.example.smartwardrobe.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for caching 3D model files locally to avoid regenerating them.
 * Downloads GLB files and stores them in app's cache directory.
 */
class ModelCacheRepository(private val context: Context) {

    companion object {
        private const val TAG = "ModelCache"
        private const val CACHE_DIR_NAME = "3d_models"
    }

    private val client = OkHttpClient()

    /**
     * Gets the cache directory for 3D models
     */
    private fun getCacheDir(): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Downloads a 3D model from URL and caches it locally.
     * Returns the local file path.
     */
    suspend fun cacheModel(modelUrl: String, modelId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Starting model cache process ===")
                Log.d(TAG, "Model ID: $modelId")
                Log.d(TAG, "Model URL: $modelUrl")

                // Create local file
                val fileName = "${modelId}.glb"
                val localFile = File(getCacheDir(), fileName)
                Log.d(TAG, "Local cache path: ${localFile.absolutePath}")

                // If already cached, return existing file
                if (localFile.exists()) {
                    Log.d(TAG, "✓ Model already cached, using existing file (${localFile.length()} bytes)")
                    return@withContext Result.success(localFile.absolutePath)
                }

                Log.d(TAG, "Downloading model from URL...")

                // Download the model
                val request = Request.Builder()
                    .url(modelUrl)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Download response code: ${response.code}")

                    if (!response.isSuccessful) {
                        throw Exception("Failed to download model: HTTP ${response.code}")
                    }

                    val contentLength = response.body?.contentLength() ?: 0
                    Log.d(TAG, "Downloading ${contentLength} bytes...")

                    // Save to local file
                    FileOutputStream(localFile).use { output ->
                        val bytes = response.body?.byteStream()?.copyTo(output)
                            ?: throw Exception("Empty response body")
                        Log.d(TAG, "Wrote $bytes bytes to file")
                    }

                    Log.d(TAG, "✓ Model cached successfully at: ${localFile.absolutePath}")
                    Log.d(TAG, "File size: ${localFile.length()} bytes")
                    Log.d(TAG, "=== Cache process complete ===")

                    Result.success(localFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error caching model: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Gets a cached model file path if it exists
     */
    fun getCachedModelPath(modelId: String): String? {
        val fileName = "${modelId}.glb"
        val localFile = File(getCacheDir(), fileName)
        return if (localFile.exists()) {
            localFile.absolutePath
        } else {
            null
        }
    }

    /**
     * Checks if a model is cached
     */
    fun isModelCached(modelId: String): Boolean {
        return getCachedModelPath(modelId) != null
    }

    /**
     * Clears all cached models
     */
    fun clearCache() {
        getCacheDir().listFiles()?.forEach { it.delete() }
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Gets total cache size in bytes
     */
    fun getCacheSize(): Long {
        return getCacheDir().listFiles()?.sumOf { it.length() } ?: 0L
    }
}

package com.example.smartwardrobe.data.repository

import android.content.Context
import android.net.Uri
import com.example.smartwardrobe.data.util.Result
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Repository for storing images locally on the device.
 * This is a free alternative to Firebase Storage.
 */
class LocalStorageRepository(private val context: Context) {

    private val imageDir: File by lazy {
        File(context.filesDir, "wardrobe_images").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    /**
     * Save an image from Uri to local storage.
     * Returns the local file path as a string.
     */
    fun saveImage(uri: Uri): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(imageDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Could not open image")

            Result.Success(file.absolutePath)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Delete an image from local storage.
     */
    fun deleteImage(path: String): Result<Unit> {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get all stored images (for debugging/cleanup).
     */
    fun getAllImages(): List<String> {
        return imageDir.listFiles()?.map { it.absolutePath } ?: emptyList()
    }

    /**
     * Clear all stored images.
     */
    fun clearAllImages(): Result<Unit> {
        return try {
            imageDir.listFiles()?.forEach { it.delete() }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
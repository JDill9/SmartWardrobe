package com.example.smartwardrobe.data.repository

import android.net.Uri
import com.example.smartwardrobe.data.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    /**
     * Upload a clothing image to Firebase Storage
     * Returns the public download URL or an error
     */
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val userId = getCurrentUserId()
            val fileName = "wardrobe/${userId}/${System.currentTimeMillis()}.jpg"
            val ref = storage.reference.child(fileName)

            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    /**
     * Upload image bytes (e.g., outfit preview) to Firebase Storage
     * Returns the download URL.
     */
    suspend fun uploadImageBytes(path: String, bytes: ByteArray): Result<String> {
        return try {
            val userId = getCurrentUserId()
            val ref = storage.reference.child("outfits/$userId/$path")

            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Optional: Delete an image from Firebase Storage
     */
    suspend fun deleteImage(url: String): Result<Unit> {
        return try {
            val ref = storage.getReferenceFromUrl(url)
            ref.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
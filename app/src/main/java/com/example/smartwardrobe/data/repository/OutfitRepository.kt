package com.example.smartwardrobe.data.repository
import java.util.Date
import android.net.Uri

import com.example.smartwardrobe.data.model.Outfit
import com.example.smartwardrobe.data.model.Season
import com.example.smartwardrobe.data.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for Outfit operations
 * Handles CRUD operations for outfits (combinations of wardrobe items)
 */
class OutfitRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val outfitCollection = firestore.collection("outfits")

    /**
     * Get current user's ID or throw exception
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    /**
     * Create a new outfit
     */
    suspend fun createOutfit(outfit: Outfit): Result<Outfit> {
        return try {
            val userId = getCurrentUserId()
            val now = Date()

            val outfitWithUserId = outfit.copy(
                userId = userId,
                createdAt = now,
                updatedAt = now
            )


            val docRef = outfitCollection.add(outfitWithUserId).await()
            val createdOutfit = outfitWithUserId.copy(id = docRef.id)
            
            // Update document with its own ID
            docRef.set(createdOutfit).await()
            
            Result.Success(createdOutfit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Create a new outfit WITH uploaded image
     */
    suspend fun createOutfitWithImage(
        outfit: Outfit,
        imageUri: Uri
    ): Result<Outfit> {
        return try {
            val userId = getCurrentUserId()

            // Upload image first
            val storageRepo = StorageRepository()
            val uploadResult = storageRepo.uploadImage(imageUri)

            if (uploadResult !is Result.Success) {
                return Result.Error(Exception("Image upload failed"))
            }

            val imageUrl = uploadResult.data

            // Add user + image URL to outfit
            val outfitWithData = outfit.copy(
                userId = userId,
                imageUrl = imageUrl,
                createdAt = Date(),
                updatedAt = Date()
            )

            // Save outfit to Firestore
            val docRef = outfitCollection.add(outfitWithData).await()
            val savedOutfit = outfitWithData.copy(id = docRef.id)

            docRef.set(savedOutfit).await()

            Result.Success(savedOutfit)

        } catch (e: Exception) {
            Result.Error(e)
        }
    }


    /**
     * Get an outfit by ID
     */
    suspend fun getOutfit(outfitId: String): Result<Outfit> {
        return try {
            val userId = getCurrentUserId()
            val doc = outfitCollection.document(outfitId).get().await()
            val outfit = doc.toObject(Outfit::class.java)
                ?: throw Exception("Outfit not found")

            // Verify ownership
            if (outfit.userId != userId) {
                throw Exception("Unauthorized access to outfit")
            }

            Result.Success(outfit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get all outfits for current user as a Flow
     * Updates in real-time when outfits are added/modified/deleted
     */
    fun getAllOutfitsFlow(): Flow<Result<List<Outfit>>> = callbackFlow {
        try {
            val userId = getCurrentUserId()
            val listener = outfitCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }

                    val outfits = snapshot?.documents?.mapNotNull { 
                        it.toObject(Outfit::class.java) 
                    } ?: emptyList()

                    trySend(Result.Success(outfits))
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(Result.Error(e))
            awaitClose { }
        }
    }

    /**
     * Get all outfits for current user (one-time fetch)
     */
    suspend fun getAllOutfits(): Result<List<Outfit>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = outfitCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val outfits = snapshot.documents.mapNotNull { 
                it.toObject(Outfit::class.java) 
            }

            Result.Success(outfits)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get favorite outfits
     */
    suspend fun getFavoriteOutfits(): Result<List<Outfit>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = outfitCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFavorite", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val outfits = snapshot.documents.mapNotNull { 
                it.toObject(Outfit::class.java) 
            }

            Result.Success(outfits)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get outfits by season
     */
    suspend fun getOutfitsBySeason(season: Season): Result<List<Outfit>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = outfitCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("season", season.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val outfits = snapshot.documents.mapNotNull { 
                it.toObject(Outfit::class.java) 
            }

            Result.Success(outfits)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Search outfits by name or occasion
     */
    suspend fun searchOutfits(query: String): Result<List<Outfit>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = outfitCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val outfits = snapshot.documents
                .mapNotNull { it.toObject(Outfit::class.java) }
                .filter { outfit ->
                    val searchQuery = query.lowercase()
                    outfit.name.lowercase().contains(searchQuery) ||
                    outfit.occasion.any { it.lowercase().contains(searchQuery) }
                }

            Result.Success(outfits)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Update an outfit
     */
    suspend fun updateOutfit(outfit: Outfit): Result<Outfit> {
        return try {
            val userId = getCurrentUserId()
            
            // Verify ownership
            if (outfit.userId != userId) {
                throw Exception("Unauthorized: Cannot update another user's outfit")
            }

            val updated = outfit.copy(updatedAt = Date())
            outfitCollection.document(outfit.id).set(updated).await()
            Result.Success(updated)
            Result.Success(outfit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Toggle favorite status of an outfit
     */
    suspend fun toggleFavorite(outfitId: String): Result<Outfit> {
        return try {
            val userId = getCurrentUserId()
            val doc = outfitCollection.document(outfitId).get().await()
            val outfit = doc.toObject(Outfit::class.java)
                ?: throw Exception("Outfit not found")

            // Verify ownership
            if (outfit.userId != userId) {
                throw Exception("Unauthorized: Cannot modify another user's outfit")
            }

            val updatedOutfit = outfit.copy(isFavorite = !outfit.isFavorite)
            outfitCollection.document(outfitId).set(updatedOutfit).await()
            
            Result.Success(updatedOutfit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Delete an outfit
     */
    suspend fun deleteOutfit(outfitId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            
            // Verify ownership before deletion
            val doc = outfitCollection.document(outfitId).get().await()
            val outfit = doc.toObject(Outfit::class.java)
                ?: throw Exception("Outfit not found")

            if (outfit.userId != userId) {
                throw Exception("Unauthorized: Cannot delete another user's outfit")
            }

            outfitCollection.document(outfitId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get outfits containing a specific wardrobe item
     * Useful when deleting an item to update associated outfits
     */
    suspend fun getOutfitsContainingItem(itemId: String): Result<List<Outfit>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = outfitCollection
                .whereEqualTo("userId", userId)
                .whereArrayContains("itemIds", itemId)
                .get()
                .await()

            val outfits = snapshot.documents.mapNotNull { 
                it.toObject(Outfit::class.java) 
            }

            Result.Success(outfits)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

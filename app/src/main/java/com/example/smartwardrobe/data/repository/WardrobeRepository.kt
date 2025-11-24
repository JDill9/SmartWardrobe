package com.example.smartwardrobe.data.repository
import android.net.Uri
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository for Wardrobe operations
 * Handles CRUD operations for wardrobe items
 */
class WardrobeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storageRepo: StorageRepository = StorageRepository()
) {
    private val wardrobeCollection = firestore.collection("wardrobeItems")

    /**
     * Get current user's ID or throw exception
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    /**
     * Add a new wardrobe item
     */
    suspend fun addWardrobeItem(item: WardrobeItem): Result<WardrobeItem> {
        return try {
            val userId = getCurrentUserId()
            val itemWithUserId = item.copy(userId = userId)
            
            val docRef = wardrobeCollection.add(itemWithUserId).await()
            val createdItem = itemWithUserId.copy(id = docRef.id)
            
            // Update document with its own ID
            docRef.set(createdItem).await()
            
            Result.Success(createdItem)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun addWardrobeItemWithImage(
        item: WardrobeItem,
        imageUri: Uri
    ): Result<WardrobeItem> {
        return try {
            val userId = getCurrentUserId()

            // 1. Upload image to Storage
            val uploadResult = storageRepo.uploadImage(imageUri)
            if (uploadResult !is Result.Success) {
                return Result.Error(Exception("Image upload failed"))
            }

            val imageUrl = uploadResult.data

            // 2. Save item to Firestore
            val itemWithData = item.copy(
                userId = userId,
                imageUrl = imageUrl,
                createdAt = Date(),
                updatedAt = Date()

            )

            val docRef = wardrobeCollection.add(itemWithData).await()
            val savedItem = itemWithData.copy(id = docRef.id)

            docRef.set(savedItem).await()

            Result.Success(savedItem)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }


    /**
     * Get a wardrobe item by ID
     */
    suspend fun getWardrobeItem(itemId: String): Result<WardrobeItem> {
        return try {
            val userId = getCurrentUserId()
            val doc = wardrobeCollection.document(itemId).get().await()
            val item = doc.toObject(WardrobeItem::class.java)
                ?: throw Exception("Item not found")

            // Verify ownership
            if (item.userId != userId) {
                throw Exception("Unauthorized access to item")
            }

            Result.Success(item)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get all wardrobe items for current user as a Flow
     * Updates in real-time when items are added/modified/deleted
     */
    fun getAllWardrobeItemsFlow(): Flow<Result<List<WardrobeItem>>> = callbackFlow {
        try {
            val userId = getCurrentUserId()
            val listener = wardrobeCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Error(error))
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents?.mapNotNull { 
                        it.toObject(WardrobeItem::class.java) 
                    } ?: emptyList()

                    trySend(Result.Success(items))
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(Result.Error(e))
            awaitClose { }
        }
    }

    /**
     * Get all wardrobe items for current user (one-time fetch)
     */
    suspend fun getAllWardrobeItems(): Result<List<WardrobeItem>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = wardrobeCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { 
                it.toObject(WardrobeItem::class.java) 
            }

            Result.Success(items)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get wardrobe items by category
     */
    suspend fun getWardrobeItemsByCategory(category: ClothingCategory): Result<List<WardrobeItem>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = wardrobeCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { 
                it.toObject(WardrobeItem::class.java) 
            }

            Result.Success(items)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Search wardrobe items by name, brand, or tags
     */
    suspend fun searchWardrobeItems(query: String): Result<List<WardrobeItem>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = wardrobeCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val items = snapshot.documents
                .mapNotNull { it.toObject(WardrobeItem::class.java) }
                .filter { item ->
                    val searchQuery = query.lowercase()
                    item.name.lowercase().contains(searchQuery) ||
                    item.brand?.lowercase()?.contains(searchQuery) == true ||
                    item.tags.any { it.lowercase().contains(searchQuery) } ||
                    item.color.lowercase().contains(searchQuery)
                }

            Result.Success(items)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Update a wardrobe item
     */
    suspend fun updateWardrobeItem(item: WardrobeItem): Result<WardrobeItem> {
        return try {
            val userId = getCurrentUserId()
            
            // Verify ownership
            if (item.userId != userId) {
                throw Exception("Unauthorized: Cannot update another user's item")
            }

            wardrobeCollection.document(item.id).set(item).await()
            Result.Success(item)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Delete a wardrobe item
     */
    suspend fun deleteWardrobeItem(itemId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            
            // Verify ownership before deletion
            val doc = wardrobeCollection.document(itemId).get().await()
            val item = doc.toObject(WardrobeItem::class.java)
                ?: throw Exception("Item not found")

            if (item.userId != userId) {
                throw Exception("Unauthorized: Cannot delete another user's item")
            }

            wardrobeCollection.document(itemId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Get wardrobe statistics (count by category)
     */
    suspend fun getWardrobeStats(): Result<Map<ClothingCategory, Int>> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = wardrobeCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { 
                it.toObject(WardrobeItem::class.java) 
            }

            val stats = ClothingCategory.values().associateWith { category ->
                items.count { it.category == category }
            }

            Result.Success(stats)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

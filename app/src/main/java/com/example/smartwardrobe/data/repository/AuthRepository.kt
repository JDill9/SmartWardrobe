package com.example.smartwardrobe.data.repository

import com.example.smartwardrobe.data.model.User
import com.example.smartwardrobe.data.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for Firebase Authentication operations
 * Handles user registration, login, logout, and profile management
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    /**
     * Get the current authenticated user as a Flow
     * Emits whenever auth state changes
     */
    fun getCurrentUserFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    /**
     * Get the current authenticated user (one-time)
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Register a new user with email and password
     * Automatically creates a user document in Firestore
     */
    suspend fun registerUser(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return try {
            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName
            )
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

            // Fetch user data from Firestore
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java) 
                ?: throw Exception("User document not found")

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Update user profile (display name and/or photo)
     */
    suspend fun updateUserProfile(
        displayName: String? = null,
        photoUrl: String? = null
    ): Result<User> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("No user logged in")

            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()

            currentUser.updateProfile(profileUpdates).await()

            // Update Firestore document
            val updates = mutableMapOf<String, Any>()
            displayName?.let { updates["displayName"] = it }
            photoUrl?.let { updates["photoUrl"] = it }

            firestore.collection("users")
                .document(currentUser.uid)
                .update(updates)
                .await()

            // Fetch updated user
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java) 
                ?: throw Exception("User document not found")

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Delete user account (both Auth and Firestore data)
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("No user logged in")
            val userId = currentUser.uid

            // Delete Firestore user document
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()

            // Delete all user's wardrobe items
            val wardrobeItems = firestore.collection("wardrobeItems")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            wardrobeItems.documents.forEach { it.reference.delete() }

            // Delete all user's outfits
            val outfits = firestore.collection("outfits")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            outfits.documents.forEach { it.reference.delete() }

            // Delete Firebase Auth account
            currentUser.delete().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

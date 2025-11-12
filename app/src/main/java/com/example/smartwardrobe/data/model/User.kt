package com.example.smartwardrobe.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * User data model for Firebase Firestore
 * @property uid Firebase Authentication user ID (document ID)
 * @property email User's email address
 * @property displayName User's display name
 * @property photoUrl URL to user's profile photo (optional)
 * @property createdAt Account creation timestamp
 * @property updatedAt Last update timestamp
 */
data class User(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

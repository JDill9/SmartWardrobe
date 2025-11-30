package com.example.smartwardrobe.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Outfit representing a combination of wardrobe items
 * @property id Firestore document ID
 * @property userId Owner's Firebase Auth UID
 * @property name Outfit name (e.g., "Summer Casual", "Office Look")
 * @property itemIds List of WardrobeItem IDs included in this outfit
 * @property occasion Occasion tags (e.g., "work", "party", "gym")
 * @property season Preferred season (optional)
 * @property isFavorite Whether this outfit is marked as favorite
 * @property createdAt Outfit creation timestamp
 * @property updatedAt Last update timestamp
 */
data class Outfit(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val itemIds: List<String> = emptyList(),
    val occasion: List<String> = emptyList(),
    val season: Season? = null,
    val isFavorite: Boolean = false,
    val imageUrl: String = "",  //added for image
    val ai3DModelUrl: String? = null,  // URL to generated 3D model (optional)
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

/**
 * Season enum for outfit filtering
 */
enum class Season {
    SPRING,
    SUMMER,
    FALL,
    WINTER,
    ALL_SEASON
}

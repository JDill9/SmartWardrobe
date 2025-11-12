package com.example.smartwardrobe.data

import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.Outfit
import com.example.smartwardrobe.data.model.Season
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.repository.AuthRepository
import com.example.smartwardrobe.data.repository.OutfitRepository
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.flow.first

/**
 * Demo class showing how to use the database repositories without a UI
 * This can be called from MainActivity or any other component for testing
 */
class DatabaseDemo {
    private val authRepository = AuthRepository()
    private val wardrobeRepository = WardrobeRepository()
    private val outfitRepository = OutfitRepository()

    /**
     * Example 1: User Registration and Login
     */
    suspend fun demoUserAuthentication(): String {
        val logs = StringBuilder()
        
        logs.appendLine("=== User Authentication Demo ===\n")
        
        // Register a new user
        logs.appendLine("1. Registering new user...")
        val registerResult = authRepository.registerUser(
            email = "testuser@smartwardrobe.com",
            password = "SecurePass123!",
            displayName = "Test User"
        )
        
        when (registerResult) {
            is Result.Success -> {
                logs.appendLine("✓ User registered successfully: ${registerResult.data.displayName}")
                logs.appendLine("  Email: ${registerResult.data.email}")
                logs.appendLine("  UID: ${registerResult.data.uid}\n")
            }
            is Result.Error -> {
                logs.appendLine("✗ Registration failed: ${registerResult.exception.message}\n")
            }
            else -> {}
        }
        
        // Sign out
        logs.appendLine("2. Signing out...")
        authRepository.signOut()
        logs.appendLine("✓ Signed out successfully\n")
        
        // Sign in
        logs.appendLine("3. Signing in...")
        val signInResult = authRepository.signIn(
            email = "testuser@smartwardrobe.com",
            password = "SecurePass123!"
        )
        
        when (signInResult) {
            is Result.Success -> {
                logs.appendLine("✓ Signed in successfully: ${signInResult.data.displayName}\n")
            }
            is Result.Error -> {
                logs.appendLine("✗ Sign in failed: ${signInResult.exception.message}\n")
            }
            else -> {}
        }
        
        return logs.toString()
    }

    /**
     * Example 2: Managing Wardrobe Items
     */
    suspend fun demoWardrobeOperations(): String {
        val logs = StringBuilder()
        
        logs.appendLine("=== Wardrobe Operations Demo ===\n")
        
        // Add wardrobe items
        logs.appendLine("1. Adding wardrobe items...")
        
        val items = listOf(
            WardrobeItem(
                name = "Blue Jeans",
                category = ClothingCategory.BOTTOM,
                color = "Blue",
                brand = "Levi's",
                size = "32",
                imageUrl = "https://example.com/blue-jeans.jpg",
                tags = listOf("casual", "denim")
            ),
            WardrobeItem(
                name = "White T-Shirt",
                category = ClothingCategory.TOP,
                color = "White",
                brand = "Nike",
                size = "M",
                imageUrl = "https://example.com/white-tshirt.jpg",
                tags = listOf("casual", "basic")
            ),
            WardrobeItem(
                name = "Black Blazer",
                category = ClothingCategory.OUTERWEAR,
                color = "Black",
                brand = "Hugo Boss",
                size = "L",
                imageUrl = "https://example.com/black-blazer.jpg",
                tags = listOf("formal", "business")
            )
        )
        
        val addedItemIds = mutableListOf<String>()
        items.forEach { item ->
            val result = wardrobeRepository.addWardrobeItem(item)
            when (result) {
                is Result.Success -> {
                    logs.appendLine("✓ Added: ${result.data.name} (ID: ${result.data.id})")
                    addedItemIds.add(result.data.id)
                }
                is Result.Error -> {
                    logs.appendLine("✗ Failed to add ${item.name}: ${result.exception.message}")
                }
                else -> {}
            }
        }
        logs.appendLine()
        
        // Get all wardrobe items
        logs.appendLine("2. Fetching all wardrobe items...")
        val allItemsResult = wardrobeRepository.getAllWardrobeItems()
        when (allItemsResult) {
            is Result.Success -> {
                logs.appendLine("✓ Found ${allItemsResult.data.size} items:")
                allItemsResult.data.forEach { item ->
                    logs.appendLine("  - ${item.name} (${item.category})")
                }
                logs.appendLine()
            }
            is Result.Error -> {
                logs.appendLine("✗ Failed to fetch items: ${allItemsResult.exception.message}\n")
            }
            else -> {}
        }
        
        // Search items
        logs.appendLine("3. Searching for 'black' items...")
        val searchResult = wardrobeRepository.searchWardrobeItems("black")
        when (searchResult) {
            is Result.Success -> {
                logs.appendLine("✓ Found ${searchResult.data.size} matching items:")
                searchResult.data.forEach { item ->
                    logs.appendLine("  - ${item.name}")
                }
                logs.appendLine()
            }
            is Result.Error -> {
                logs.appendLine("✗ Search failed: ${searchResult.exception.message}\n")
            }
            else -> {}
        }
        
        // Get category statistics
        logs.appendLine("4. Getting wardrobe statistics...")
        val statsResult = wardrobeRepository.getWardrobeStats()
        when (statsResult) {
            is Result.Success -> {
                logs.appendLine("✓ Wardrobe Statistics:")
                statsResult.data.forEach { (category, count) ->
                    if (count > 0) {
                        logs.appendLine("  - $category: $count items")
                    }
                }
                logs.appendLine()
            }
            is Result.Error -> {
                logs.appendLine("✗ Stats failed: ${statsResult.exception.message}\n")
            }
            else -> {}
        }
        
        return logs.toString()
    }

    /**
     * Example 3: Creating and Managing Outfits
     */
    suspend fun demoOutfitOperations(): String {
        val logs = StringBuilder()
        
        logs.appendLine("=== Outfit Operations Demo ===\n")
        
        // First, get wardrobe items to create outfits
        logs.appendLine("1. Fetching wardrobe items for outfit creation...")
        val itemsResult = wardrobeRepository.getAllWardrobeItems()
        
        val itemIds = when (itemsResult) {
            is Result.Success -> {
                logs.appendLine("✓ Found ${itemsResult.data.size} items\n")
                itemsResult.data.map { it.id }
            }
            is Result.Error -> {
                logs.appendLine("✗ Failed to fetch items: ${itemsResult.exception.message}\n")
                return logs.toString()
            }
            else -> emptyList()
        }
        
        if (itemIds.isEmpty()) {
            logs.appendLine("⚠ No wardrobe items available. Add items first!\n")
            return logs.toString()
        }
        
        // Create outfits
        logs.appendLine("2. Creating outfits...")
        val outfits = listOf(
            Outfit(
                name = "Casual Weekend",
                itemIds = itemIds.take(2),
                occasion = listOf("casual", "weekend"),
                season = Season.SUMMER,
                isFavorite = true
            ),
            Outfit(
                name = "Business Meeting",
                itemIds = itemIds.takeLast(2),
                occasion = listOf("work", "formal"),
                season = Season.ALL_SEASON,
                isFavorite = false
            )
        )
        
        outfits.forEach { outfit ->
            val result = outfitRepository.createOutfit(outfit)
            when (result) {
                is Result.Success -> {
                    logs.appendLine("✓ Created: ${result.data.name} (${result.data.itemIds.size} items)")
                }
                is Result.Error -> {
                    logs.appendLine("✗ Failed to create ${outfit.name}: ${result.exception.message}")
                }
                else -> {}
            }
        }
        logs.appendLine()
        
        // Get all outfits
        logs.appendLine("3. Fetching all outfits...")
        val allOutfitsResult = outfitRepository.getAllOutfits()
        when (allOutfitsResult) {
            is Result.Success -> {
                logs.appendLine("✓ Found ${allOutfitsResult.data.size} outfits:")
                allOutfitsResult.data.forEach { outfit ->
                    val favIcon = if (outfit.isFavorite) "★" else "☆"
                    logs.appendLine("  $favIcon ${outfit.name} - ${outfit.itemIds.size} items")
                }
                logs.appendLine()
            }
            is Result.Error -> {
                logs.appendLine("✗ Failed to fetch outfits: ${allOutfitsResult.exception.message}\n")
            }
            else -> {}
        }
        
        // Get favorites
        logs.appendLine("4. Fetching favorite outfits...")
        val favoritesResult = outfitRepository.getFavoriteOutfits()
        when (favoritesResult) {
            is Result.Success -> {
                logs.appendLine("✓ Found ${favoritesResult.data.size} favorite outfits:")
                favoritesResult.data.forEach { outfit ->
                    logs.appendLine("  ★ ${outfit.name}")
                }
                logs.appendLine()
            }
            is Result.Error -> {
                logs.appendLine("✗ Failed to fetch favorites: ${favoritesResult.exception.message}\n")
            }
            else -> {}
        }
        
        return logs.toString()
    }

    /**
     * Run all demos sequentially
     */
    suspend fun runAllDemos(): String {
        val logs = StringBuilder()
        logs.appendLine("╔════════════════════════════════════════════════╗")
        logs.appendLine("║   SmartWardrobe Database Demo                 ║")
        logs.appendLine("╚════════════════════════════════════════════════╝\n")
        
        logs.append(demoUserAuthentication())
        logs.appendLine("\n" + "=".repeat(50) + "\n")
        
        logs.append(demoWardrobeOperations())
        logs.appendLine("\n" + "=".repeat(50) + "\n")
        
        logs.append(demoOutfitOperations())
        
        logs.appendLine("\n╔════════════════════════════════════════════════╗")
        logs.appendLine("║   Demo Complete!                              ║")
        logs.appendLine("╚════════════════════════════════════════════════╝")
        
        return logs.toString()
    }
}

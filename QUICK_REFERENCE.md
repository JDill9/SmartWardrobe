# SmartWardrobe Database - Quick Reference

## 🚀 Quick Start (After Firebase Setup)

```kotlin
// 1. Import what you need
import com.example.smartwardrobe.data.repository.*
import com.example.smartwardrobe.data.model.*
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.*

// 2. Create repository instances
val authRepo = AuthRepository()
val wardrobeRepo = WardrobeRepository()
val outfitRepo = OutfitRepository()

// 3. Use coroutines for async operations
CoroutineScope(Dispatchers.IO).launch {
    // Your database operations here
}
```

## 🔐 Authentication Cheat Sheet

```kotlin
// Register
val result = authRepo.registerUser("email@example.com", "password", "Name")

// Login
val result = authRepo.signIn("email@example.com", "password")

// Logout
authRepo.signOut()

// Get current user
val user = authRepo.getCurrentUser()

// Listen to auth changes (Flow)
authRepo.getCurrentUserFlow().collect { firebaseUser ->
    // firebaseUser is null when logged out
}

// Update profile
val result = authRepo.updateUserProfile(displayName = "New Name")

// Password reset
val result = authRepo.sendPasswordResetEmail("email@example.com")

// Delete account (WARNING: Deletes all user data!)
val result = authRepo.deleteAccount()
```

## 👕 Wardrobe Operations Cheat Sheet

```kotlin
// Add item
val item = WardrobeItem(
    name = "Blue Jeans",
    category = ClothingCategory.BOTTOM,
    color = "Blue",
    size = "32",
    imageUrl = "url",
    tags = listOf("casual")
)
val result = wardrobeRepo.addWardrobeItem(item)

// Get all items (one-time)
val result = wardrobeRepo.getAllWardrobeItems()

// Get all items (real-time Flow)
wardrobeRepo.getAllWardrobeItemsFlow().collect { result ->
    when (result) {
        is Result.Success -> items = result.data
        is Result.Error -> error = result.exception.message
        else -> {}
    }
}

// Get item by ID
val result = wardrobeRepo.getWardrobeItem(itemId)

// Get by category
val result = wardrobeRepo.getWardrobeItemsByCategory(ClothingCategory.TOP)

// Search
val result = wardrobeRepo.searchWardrobeItems("blue")

// Update
val result = wardrobeRepo.updateWardrobeItem(updatedItem)

// Delete
val result = wardrobeRepo.deleteWardrobeItem(itemId)

// Get statistics
val result = wardrobeRepo.getWardrobeStats()
// Returns: Map<ClothingCategory, Int>
```

## 👔 Outfit Operations Cheat Sheet

```kotlin
// Create outfit
val outfit = Outfit(
    name = "Summer Casual",
    itemIds = listOf("id1", "id2", "id3"),
    occasion = listOf("casual", "weekend"),
    season = Season.SUMMER,
    isFavorite = false
)
val result = outfitRepo.createOutfit(outfit)

// Get all outfits (one-time)
val result = outfitRepo.getAllOutfits()

// Get all outfits (real-time Flow)
outfitRepo.getAllOutfitsFlow().collect { result -> }

// Get outfit by ID
val result = outfitRepo.getOutfit(outfitId)

// Get favorites
val result = outfitRepo.getFavoriteOutfits()

// Get by season
val result = outfitRepo.getOutfitsBySeason(Season.SUMMER)

// Search
val result = outfitRepo.searchOutfits("casual")

// Update
val result = outfitRepo.updateOutfit(updatedOutfit)

// Toggle favorite
val result = outfitRepo.toggleFavorite(outfitId)

// Delete
val result = outfitRepo.deleteOutfit(outfitId)

// Get outfits containing specific item
val result = outfitRepo.getOutfitsContainingItem(itemId)
```

## 📊 Data Models Quick Reference

### ClothingCategory (Enum)
- `TOP` - Shirts, blouses, sweaters
- `BOTTOM` - Pants, skirts, shorts
- `DRESS` - Dresses, jumpsuits
- `OUTERWEAR` - Jackets, coats
- `SHOES` - All footwear
- `ACCESSORIES` - Bags, jewelry, belts
- `OTHER` - Miscellaneous

### Season (Enum)
- `SPRING`
- `SUMMER`
- `FALL`
- `WINTER`
- `ALL_SEASON`

### Result<T> (Sealed Class)
```kotlin
when (result) {
    is Result.Success -> result.data      // Type T
    is Result.Error -> result.exception   // Exception
    is Result.Loading -> { /* show loading */ }
}

// Helper methods
result.isSuccess   // Boolean
result.isError     // Boolean
result.isLoading   // Boolean
result.getOrNull() // T or null
result.exceptionOrNull() // Exception or null
```

## 🎯 Common Patterns

### Pattern 1: Load Data on Screen Open
```kotlin
LaunchedEffect(Unit) {
    wardrobeRepo.getAllWardrobeItemsFlow().collect { result ->
        when (result) {
            is Result.Success -> items = result.data
            is Result.Error -> showError(result.exception.message)
            else -> {}
        }
    }
}
```

### Pattern 2: Button Click Action
```kotlin
Button(onClick = {
    scope.launch {
        val result = wardrobeRepo.deleteWardrobeItem(itemId)
        when (result) {
            is Result.Success -> showToast("Deleted!")
            is Result.Error -> showError(result.exception.message)
            else -> {}
        }
    }
}) { Text("Delete") }
```

### Pattern 3: Form Submission
```kotlin
fun onSubmit() {
    scope.launch {
        val item = WardrobeItem(
            name = nameField.value,
            category = selectedCategory,
            color = colorField.value,
            size = sizeField.value,
            imageUrl = uploadedImageUrl,
            tags = tagsField.value.split(",")
        )
        
        val result = wardrobeRepo.addWardrobeItem(item)
        when (result) {
            is Result.Success -> {
                showToast("Item added!")
                navigateBack()
            }
            is Result.Error -> showError(result.exception.message)
            else -> {}
        }
    }
}
```

### Pattern 4: Search with Debounce
```kotlin
var searchQuery by remember { mutableStateOf("") }
var searchResults by remember { mutableStateOf<List<WardrobeItem>>(emptyList()) }

LaunchedEffect(searchQuery) {
    delay(300) // Debounce
    if (searchQuery.isNotEmpty()) {
        val result = wardrobeRepo.searchWardrobeItems(searchQuery)
        if (result is Result.Success) {
            searchResults = result.data
        }
    }
}
```

## ⚠️ Common Mistakes to Avoid

❌ **Don't do this:**
```kotlin
// Wrong: Calling suspend function without coroutine
val result = wardrobeRepo.addWardrobeItem(item) // Compile error!
```

✅ **Do this:**
```kotlin
// Correct: Use coroutine scope
CoroutineScope(Dispatchers.IO).launch {
    val result = wardrobeRepo.addWardrobeItem(item)
}
```

---

❌ **Don't do this:**
```kotlin
// Wrong: Accessing data before checking result type
val items = result.data // Could be null/crash!
```

✅ **Do this:**
```kotlin
// Correct: Check result type first
when (result) {
    is Result.Success -> val items = result.data
    is Result.Error -> handleError()
    else -> {}
}
```

---

❌ **Don't do this:**
```kotlin
// Wrong: Using IO dispatcher for UI updates
CoroutineScope(Dispatchers.IO).launch {
    items = result.data // Could crash UI!
}
```

✅ **Do this:**
```kotlin
// Correct: Use Main dispatcher for UI updates
CoroutineScope(Dispatchers.Main).launch {
    val result = withContext(Dispatchers.IO) {
        wardrobeRepo.getAllWardrobeItems()
    }
    if (result is Result.Success) {
        items = result.data // Safe on Main thread
    }
}
```

## 🔍 Debugging Tips

```kotlin
// Print result for debugging
when (result) {
    is Result.Success -> Log.d("Database", "Success: ${result.data}")
    is Result.Error -> Log.e("Database", "Error: ${result.exception.message}")
    else -> Log.d("Database", "Loading...")
}

// Check if user is logged in
val user = authRepo.getCurrentUser()
Log.d("Auth", "User: ${user?.email ?: "Not logged in"}")

// Check Firestore in Firebase Console
// Go to: Firebase Console → Firestore Database → Data
```

## 📱 Testing Checklist

- [ ] Firebase project created
- [ ] `google-services.json` in `app/` folder
- [ ] Gradle sync successful
- [ ] Authentication enabled in Firebase Console
- [ ] Firestore Database created
- [ ] Security rules configured
- [ ] Test user can register
- [ ] Test user can login
- [ ] Test wardrobe item can be added
- [ ] Test outfit can be created
- [ ] Data appears in Firebase Console

## 🆘 Error Messages

| Error | Meaning | Fix |
|-------|---------|-----|
| "User not authenticated" | No user logged in | Call `authRepo.signIn()` first |
| "PERMISSION_DENIED" | Firestore security rules | Check Firebase Console rules |
| "Default FirebaseApp is not initialized" | Missing google-services.json | Add file to `app/` folder |
| "Unauthorized access to item" | Trying to access another user's data | This is working as intended (security) |
| "Item not found" | Document doesn't exist | Check if ID is correct |

---

**💡 Pro Tip**: Keep this file open while coding! Copy-paste the patterns and modify as needed.

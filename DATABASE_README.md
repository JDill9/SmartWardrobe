# SmartWardrobe Database Layer

## Overview
This database layer provides complete Firebase Authentication and Firestore integration for the SmartWardrobe app. It's designed to work independently of the UI, allowing backend development to proceed in parallel with frontend work.

## 📁 Project Structure

```
app/src/main/java/com/example/smartwardrobe/
├── data/
│   ├── model/
│   │   ├── User.kt              # User account data model
│   │   ├── WardrobeItem.kt      # Clothing item model
│   │   └── Outfit.kt            # Outfit (combination of items) model
│   ├── repository/
│   │   ├── AuthRepository.kt    # Authentication operations
│   │   ├── WardrobeRepository.kt # Wardrobe CRUD operations
│   │   └── OutfitRepository.kt  # Outfit management
│   ├── util/
│   │   └── Result.kt            # Type-safe result wrapper
│   └── DatabaseDemo.kt          # Example usage without UI
```

## 🔥 Firebase Setup Required

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app with package name: `com.example.smartwardrobe`

### 2. Download google-services.json
1. Download `google-services.json` from Firebase Console
2. Place it in the `app/` directory (same level as `build.gradle.kts`)
3. **IMPORTANT**: Add to `.gitignore` if not already there

### 3. Enable Firebase Services
In Firebase Console, enable:
- **Authentication** → Email/Password provider
- **Firestore Database** → Start in production mode
- **Storage** → For user profile photos and clothing images

### 4. Firestore Security Rules (Recommended)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Wardrobe items
    match /wardrobeItems/{itemId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // Outfits
    match /outfits/{outfitId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
  }
}
```

## 🎯 Data Models

### User
```kotlin
data class User(
    val uid: String,              // Firebase Auth UID
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
```

### WardrobeItem
```kotlin
data class WardrobeItem(
    val id: String,
    val userId: String,
    val name: String,
    val category: ClothingCategory,  // TOP, BOTTOM, DRESS, SHOES, etc.
    val color: String,
    val brand: String? = null,
    val size: String,
    val imageUrl: String,
    val ai3DModelUrl: String? = null, // For future 3D integration
    val tags: List<String>,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
```

### Outfit
```kotlin
data class Outfit(
    val id: String,
    val userId: String,
    val name: String,
    val itemIds: List<String>,       // References to WardrobeItem IDs
    val occasion: List<String>,
    val season: Season? = null,      // SPRING, SUMMER, FALL, WINTER
    val isFavorite: Boolean,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
```

## 🚀 Usage Examples

### Authentication

```kotlin
import com.example.smartwardrobe.data.repository.AuthRepository
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val authRepository = AuthRepository()

// Register a new user
CoroutineScope(Dispatchers.IO).launch {
    val result = authRepository.registerUser(
        email = "user@example.com",
        password = "SecurePassword123!",
        displayName = "John Doe"
    )
    
    when (result) {
        is Result.Success -> {
            println("User registered: ${result.data.displayName}")
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}

// Sign in
CoroutineScope(Dispatchers.IO).launch {
    val result = authRepository.signIn(
        email = "user@example.com",
        password = "SecurePassword123!"
    )
    
    when (result) {
        is Result.Success -> {
            println("Logged in: ${result.data.email}")
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}

// Sign out
authRepository.signOut()

// Listen to auth state changes (Flow)
CoroutineScope(Dispatchers.Main).launch {
    authRepository.getCurrentUserFlow().collect { firebaseUser ->
        if (firebaseUser != null) {
            println("User logged in: ${firebaseUser.email}")
        } else {
            println("User logged out")
        }
    }
}
```

### Wardrobe Operations

```kotlin
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.model.ClothingCategory

val wardrobeRepository = WardrobeRepository()

// Add a wardrobe item
CoroutineScope(Dispatchers.IO).launch {
    val item = WardrobeItem(
        name = "Blue Jeans",
        category = ClothingCategory.BOTTOM,
        color = "Blue",
        brand = "Levi's",
        size = "32",
        imageUrl = "https://example.com/photo.jpg",
        tags = listOf("casual", "denim")
    )
    
    val result = wardrobeRepository.addWardrobeItem(item)
    when (result) {
        is Result.Success -> {
            println("Item added: ${result.data.id}")
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}

// Get all wardrobe items (one-time)
CoroutineScope(Dispatchers.IO).launch {
    val result = wardrobeRepository.getAllWardrobeItems()
    when (result) {
        is Result.Success -> {
            result.data.forEach { item ->
                println("${item.name} - ${item.category}")
            }
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}

// Get all wardrobe items (real-time Flow)
CoroutineScope(Dispatchers.Main).launch {
    wardrobeRepository.getAllWardrobeItemsFlow().collect { result ->
        when (result) {
            is Result.Success -> {
                println("Total items: ${result.data.size}")
                // Update UI here
            }
            is Result.Error -> {
                println("Error: ${result.exception.message}")
            }
            else -> {}
        }
    }
}

// Search items
CoroutineScope(Dispatchers.IO).launch {
    val result = wardrobeRepository.searchWardrobeItems("blue")
    when (result) {
        is Result.Success -> {
            println("Found ${result.data.size} items")
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}

// Get items by category
CoroutineScope(Dispatchers.IO).launch {
    val result = wardrobeRepository.getWardrobeItemsByCategory(ClothingCategory.TOP)
    when (result) {
        is Result.Success -> {
            println("Found ${result.data.size} tops")
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}

// Update an item
CoroutineScope(Dispatchers.IO).launch {
    val updatedItem = item.copy(name = "Dark Blue Jeans")
    val result = wardrobeRepository.updateWardrobeItem(updatedItem)
}

// Delete an item
CoroutineScope(Dispatchers.IO).launch {
    val result = wardrobeRepository.deleteWardrobeItem(itemId)
}

// Get wardrobe statistics
CoroutineScope(Dispatchers.IO).launch {
    val result = wardrobeRepository.getWardrobeStats()
    when (result) {
        is Result.Success -> {
            result.data.forEach { (category, count) ->
                println("$category: $count items")
            }
        }
        is Result.Error -> {
            println("Error: ${result.exception.message}")
        }
        else -> {}
    }
}
```

### Outfit Operations

```kotlin
import com.example.smartwardrobe.data.repository.OutfitRepository
import com.example.smartwardrobe.data.model.Outfit
import com.example.smartwardrobe.data.model.Season

val outfitRepository = OutfitRepository()

// Create an outfit
CoroutineScope(Dispatchers.IO).launch {
    val outfit = Outfit(
        name = "Summer Casual",
        itemIds = listOf("item1Id", "item2Id", "item3Id"),
        occasion = listOf("casual", "weekend"),
        season = Season.SUMMER,
        isFavorite = false
    )
    
    val result = outfitRepository.createOutfit(outfit)
}

// Get all outfits (real-time Flow)
CoroutineScope(Dispatchers.Main).launch {
    outfitRepository.getAllOutfitsFlow().collect { result ->
        when (result) {
            is Result.Success -> {
                result.data.forEach { outfit ->
                    println("${outfit.name} - ${outfit.itemIds.size} items")
                }
            }
            is Result.Error -> {
                println("Error: ${result.exception.message}")
            }
            else -> {}
        }
    }
}

// Get favorite outfits
CoroutineScope(Dispatchers.IO).launch {
    val result = outfitRepository.getFavoriteOutfits()
}

// Toggle favorite
CoroutineScope(Dispatchers.IO).launch {
    val result = outfitRepository.toggleFavorite(outfitId)
}

// Get outfits by season
CoroutineScope(Dispatchers.IO).launch {
    val result = outfitRepository.getOutfitsBySeason(Season.SUMMER)
}

// Search outfits
CoroutineScope(Dispatchers.IO).launch {
    val result = outfitRepository.searchOutfits("casual")
}

// Delete outfit
CoroutineScope(Dispatchers.IO).launch {
    val result = outfitRepository.deleteOutfit(outfitId)
}
```

## 🧪 Testing Without UI

Use the `DatabaseDemo` class to test all functionality:

```kotlin
import com.example.smartwardrobe.data.DatabaseDemo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// In MainActivity or any component
CoroutineScope(Dispatchers.IO).launch {
    val demo = DatabaseDemo()
    val output = demo.runAllDemos()
    println(output)
    // Or use Android Logcat: Log.d("DatabaseDemo", output)
}
```

## 🔐 Security Features

- **User ownership verification**: All operations verify that users can only access their own data
- **Authentication required**: All database operations require an authenticated user
- **Type-safe error handling**: Using `Result` sealed class instead of exceptions
- **Automatic timestamps**: Firestore manages `createdAt` and `updatedAt` fields

## 📱 Integration with UI (For Teammates)

### Using with Jetpack Compose

```kotlin
@Composable
fun WardrobeScreen() {
    val wardrobeRepository = remember { WardrobeRepository() }
    var items by remember { mutableStateOf<List<WardrobeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        wardrobeRepository.getAllWardrobeItemsFlow().collect { result ->
            when (result) {
                is Result.Success -> {
                    items = result.data
                    isLoading = false
                }
                is Result.Error -> {
                    error = result.exception.message
                    isLoading = false
                }
                is Result.Loading -> {
                    isLoading = true
                }
            }
        }
    }
    
    when {
        isLoading -> CircularProgressIndicator()
        error != null -> Text("Error: $error")
        else -> {
            LazyColumn {
                items(items) { item ->
                    Text("${item.name} - ${item.category}")
                }
            }
        }
    }
}
```

### Using with ViewModel (Recommended)

```kotlin
class WardrobeViewModel : ViewModel() {
    private val wardrobeRepository = WardrobeRepository()
    
    private val _items = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val items: StateFlow<List<WardrobeItem>> = _items.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadItems()
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            wardrobeRepository.getAllWardrobeItemsFlow().collect { result ->
                when (result) {
                    is Result.Success -> _items.value = result.data
                    is Result.Error -> {
                        // Handle error
                    }
                    is Result.Loading -> _isLoading.value = true
                }
            }
        }
    }
    
    fun addItem(item: WardrobeItem) {
        viewModelScope.launch {
            wardrobeRepository.addWardrobeItem(item)
        }
    }
}
```

## 📊 Firestore Collections Structure

```
users/
  {userId}/
    - uid: String
    - email: String
    - displayName: String
    - photoUrl: String?
    - createdAt: Timestamp
    - updatedAt: Timestamp

wardrobeItems/
  {itemId}/
    - id: String
    - userId: String (indexed)
    - name: String
    - category: String (indexed)
    - color: String
    - brand: String?
    - size: String
    - imageUrl: String
    - ai3DModelUrl: String?
    - tags: Array<String>
    - createdAt: Timestamp
    - updatedAt: Timestamp

outfits/
  {outfitId}/
    - id: String
    - userId: String (indexed)
    - name: String
    - itemIds: Array<String> (array-contains indexed)
    - occasion: Array<String>
    - season: String?
    - isFavorite: Boolean (indexed)
    - createdAt: Timestamp
    - updatedAt: Timestamp
```

## 🎨 Next Steps for 3D/AI Integration

When your teammates are ready to integrate the 3D wardrobe visualization:

1. **Image Upload**: Use Firebase Storage to store clothing photos
2. **AI Processing**: Store the AI-generated 3D model URL in `WardrobeItem.ai3DModelUrl`
3. **Three.js Integration**: Fetch items and render 3D models in WebView or native 3D view
4. **Real-time Updates**: Use the Flow-based methods to keep 3D wardrobe in sync

## 📝 Notes

- All repository methods that modify data return `Result<T>` for error handling
- Use `Flow` methods for real-time UI updates
- Use suspend functions for one-time operations
- Remember to handle authentication state in your UI
- Test with Firebase Emulator during development if needed

## 🐛 Troubleshooting

**Problem**: "User not authenticated" errors
- **Solution**: Make sure user is signed in before calling wardrobe/outfit operations

**Problem**: Gradle sync fails
- **Solution**: Ensure `google-services.json` is in the `app/` directory

**Problem**: Firestore permission denied
- **Solution**: Check Firebase Console security rules and ensure user is authenticated

**Problem**: Items not appearing
- **Solution**: Check Firestore Console to verify data is being written correctly

## 👥 Team Collaboration

This database layer is **completely independent** of the UI. Your teammates can:
- Build UI components in parallel
- Mock the repositories for UI testing
- Integrate when ready using the examples above

---

**Built by**: [Your Name]  
**Last Updated**: November 12, 2025  
**Questions**: Contact me or check the code comments!

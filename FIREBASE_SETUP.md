# Firebase Setup Guide for SmartWardrobe

## Prerequisites
- Android Studio installed
- Google account for Firebase Console
- Internet connection

## Step-by-Step Setup (5-10 minutes)

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or select existing project
3. Enter project name: **"SmartWardrobe"** (or any name you prefer)
4. Accept Firebase terms and click **Continue**
5. (Optional) Enable Google Analytics - choose as you prefer
6. Click **Create project** and wait for setup to complete

### 2. Add Android App to Firebase

1. In Firebase Console, click the **Android icon** (or "Add app" button)
2. Enter the following details:
   - **Android package name**: `com.example.smartwardrobe` ⚠️ **MUST MATCH EXACTLY**
   - **App nickname** (optional): "SmartWardrobe Android"
   - **Debug signing certificate** (optional): Leave blank for now
3. Click **Register app**

### 3. Download Configuration File

1. Click **Download google-services.json**
2. Save the file to your computer
3. **IMPORTANT**: Move `google-services.json` to your project's `app/` folder:
   ```
   SmartWardrobe/
   └── app/
       ├── google-services.json  ← PUT IT HERE
       ├── build.gradle.kts
       └── src/
   ```
4. In Firebase Console, click **Next** → **Next** → **Continue to console**

### 4. Enable Firebase Services

#### Enable Authentication:
1. In Firebase Console left sidebar, click **Build** → **Authentication**
2. Click **Get Started**
3. Click **Email/Password** under "Sign-in providers"
4. Toggle **Enable** switch ON
5. Click **Save**

#### Enable Firestore Database:
1. In left sidebar, click **Build** → **Firestore Database**
2. Click **Create database**
3. Choose **Start in production mode** (we'll add security rules next)
4. Select a location (choose closest to your users)
5. Click **Enable**

#### Enable Storage (for images):
1. In left sidebar, click **Build** → **Storage**
2. Click **Get started**
3. Click **Next** (keep default rules for now)
4. Select a location (same as Firestore)
5. Click **Done**

### 5. Configure Firestore Security Rules

1. In **Firestore Database**, go to **Rules** tab
2. Replace the rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can only read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Wardrobe items - users can only access their own items
    match /wardrobeItems/{itemId} {
      allow read, update, delete: if request.auth != null && 
        resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
    
    // Outfits - users can only access their own outfits
    match /outfits/{outfitId} {
      allow read, update, delete: if request.auth != null && 
        resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
  }
}
```

3. Click **Publish**

### 6. Configure Storage Security Rules

1. In **Storage**, go to **Rules** tab
2. Replace the rules with:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

3. Click **Publish**

### 7. Sync Your Android Project

1. Open the project in Android Studio
2. Click **File** → **Sync Project with Gradle Files**
3. Wait for sync to complete (may take a few minutes first time)
4. Check for any errors in the **Build** tab

### 8. Test the Database

1. In `MainActivity.kt`, uncomment the line:
   ```kotlin
   // testDatabase()  ← Remove the //
   ```
   
2. Run the app on an emulator or physical device

3. Open **Logcat** (bottom toolbar in Android Studio)

4. Set filter to: `DatabaseDemo`

5. You should see output showing:
   - User registration
   - User login
   - Adding wardrobe items
   - Creating outfits
   - Querying data

### 9. Verify in Firebase Console

1. Go to **Authentication** → **Users** tab
   - You should see a test user created

2. Go to **Firestore Database** → **Data** tab
   - You should see collections: `users`, `wardrobeItems`, `outfits`
   - Browse the documents to see test data

## ✅ Setup Complete!

Your database layer is now fully functional and ready to use.

## 🎯 Next Steps

1. **Read DATABASE_README.md** - Complete usage guide with code examples
2. **Explore the repositories** - Look at `AuthRepository`, `WardrobeRepository`, `OutfitRepository`
3. **Integrate with UI** - When your teammates are ready, use the examples in DATABASE_README.md
4. **Add real data** - Replace test data with actual user inputs

## 🐛 Troubleshooting

### "Default FirebaseApp is not initialized"
- **Cause**: `google-services.json` not in correct location
- **Fix**: Ensure file is in `app/` folder, not `app/src/`

### Gradle sync fails with "Could not find google-services"
- **Cause**: Missing Google Services plugin
- **Fix**: Already configured, try **File** → **Invalidate Caches** → **Invalidate and Restart**

### "PERMISSION_DENIED" in Firestore
- **Cause**: Security rules not set or user not authenticated
- **Fix**: 
  1. Check Firebase Console → Firestore → Rules
  2. Ensure user is signed in before accessing data

### No data appearing in Firestore
- **Cause**: Network connectivity or authentication issue
- **Fix**: Check Logcat for error messages with filter `Firebase` or `DatabaseDemo`

### "google-services.json is missing"
- **Cause**: File not downloaded or placed correctly
- **Fix**: Re-download from Firebase Console and place in `app/` folder

## 📱 Testing on Different Devices

The same Firebase project works for all devices. Just install and run the app!

## 🔒 Security Notes

- Never commit `google-services.json` to public repositories
- The security rules ensure users can only access their own data
- For production, review and tighten security rules further
- Consider enabling App Check for additional security

## 💡 Pro Tips

1. **Use Firebase Emulator** during development to avoid using production data
2. **Enable Firebase Analytics** to track usage patterns
3. **Set up indexes** in Firestore for complex queries (Firebase will prompt you)
4. **Monitor usage** in Firebase Console to stay within free tier limits

---

**Having Issues?** Check the [Firebase Documentation](https://firebase.google.com/docs) or ask your team!

**Setup Time**: ~5-10 minutes  
**Difficulty**: Easy  
**Cost**: Free (Firebase Spark Plan)

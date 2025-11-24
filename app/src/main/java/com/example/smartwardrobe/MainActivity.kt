package com.example.smartwardrobe
import com.example.smartwardrobe.data.repository.AuthRepository
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.model.ClothingCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.smartwardrobe.ai.ImageTo3DScreenHost
import com.example.smartwardrobe.ui.theme.SmartWardrobeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       /* val db = Firebase.firestore
        val testData = hashMapOf(
            "message" to "SmartWardrobe Firebase is working",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("debugStartup")
            .add(testData)
            .addOnSuccessListener { docRef ->
                Log.d("FirebaseTest", "Document written with ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTest", "Error adding document", e)
            } */



        setContent {
            SmartWardrobeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

/* firebase test email adress and password
    email: fortnite100a@gmail.com
    password: welovefortnite
 */
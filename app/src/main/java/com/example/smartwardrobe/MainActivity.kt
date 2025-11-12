package com.example.smartwardrobe

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartwardrobe.data.DatabaseDemo
import com.example.smartwardrobe.ui.theme.SmartWardrobeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // UNCOMMENT THE LINES BELOW TO TEST DATABASE (after setting up Firebase)
        // testDatabase()
        
        setContent {
            SmartWardrobeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "SmartWardrobe",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    /**
     * Test database functionality without UI
     * Check Logcat with filter "DatabaseDemo" to see results
     * 
     * IMPORTANT: Setup Firebase first (see DATABASE_README.md)
     */
    private fun testDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val demo = DatabaseDemo()
            val output = demo.runAllDemos()
            Log.d("DatabaseDemo", output)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to $name!\n\nDatabase layer is ready.\nSee DATABASE_README.md for usage.",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartWardrobeTheme {
        Greeting("SmartWardrobe")
    }
}
package com.example.smartwardrobe

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
        setContent {
            SmartWardrobeTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Entry point for your AI/3D flow
                    ImageTo3DScreenHost()
                }
            }
        }
    }
}

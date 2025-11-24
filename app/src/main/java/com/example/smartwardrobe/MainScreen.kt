package com.example.smartwardrobe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen(rootNavController: NavHostController) {

    // This navController is ONLY for bottom navigation tabs
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(bottomNavController)
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            BottomNavGraph(
                navController = bottomNavController,
                rootNavController = rootNavController
            )
        }
    }
}

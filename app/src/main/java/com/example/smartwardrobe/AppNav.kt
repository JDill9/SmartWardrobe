package com.example.smartwardrobe

import androidx.navigation.compose.*
import androidx.compose.runtime.Composable

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            Login { email, password ->
                if (email == "test@gmail.com" && password == "123456") {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }

        composable("main") {
            MainScreen() // contains bottom navigation + inner nav host
        }
    }
}
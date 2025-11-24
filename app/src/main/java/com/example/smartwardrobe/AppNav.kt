package com.example.smartwardrobe
import androidx.compose.runtime.rememberCoroutineScope
import com.example.smartwardrobe.data.repository.AuthRepository
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.launch

import androidx.navigation.compose.*
import androidx.compose.runtime.Composable

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            val auth = AuthRepository()
            val scope = rememberCoroutineScope()

            LoginScreen(
                onLoginClick = { email, password ->
                    scope.launch {
                        val result = auth.signIn(email, password)
                        if (result is Result.Success) {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                },
                onRegisterClick = { email, password ->
                    scope.launch {
                        val result = auth.registerUser(email, password, displayName = email.substringBefore("@"))
                        if (result is Result.Success) {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }


        composable("main") {
            MainScreen(rootNavController = navController)
        }
    }
}
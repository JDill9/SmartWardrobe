package com.example.smartwardrobe

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route
    ) {

        composable(BottomNavItem.Home.route) {
            Home()
        }

        composable(BottomNavItem.Account.route) {
            Account(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()

                    rootNavController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable(BottomNavItem.Build.route) {
            Build()
        }

        composable(BottomNavItem.Saved.route) {
            Saved()
        }
    }
}

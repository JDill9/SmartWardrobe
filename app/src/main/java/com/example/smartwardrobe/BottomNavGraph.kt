package com.example.smartwardrobe

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun BottomNavGraph(navController: NavHostController) {

    NavHost(navController = navController, startDestination = BottomNavItem.Home.route) {

        composable(BottomNavItem.Home.route) {
            Home()
        }

        composable(BottomNavItem.Account.route) {
            Account({})
        }

        composable(BottomNavItem.Build.route) {
            Build()
        }

        composable(BottomNavItem.Saved.route) {
            Saved()
        }
    }
}
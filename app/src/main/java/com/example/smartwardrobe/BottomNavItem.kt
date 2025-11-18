package com.example.smartwardrobe

sealed class BottomNavItem(val route: String, val label: String) {
    object Home : BottomNavItem("home", "Home")
    object Account : BottomNavItem("account", "Account")
    object Build : BottomNavItem("build", "Build")
    object Saved : BottomNavItem("saved", "Saved")
}
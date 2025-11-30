package com.example.smartwardrobe


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState



@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Build,
        BottomNavItem.Saved,
        BottomNavItem.Account
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        items.forEach { item ->
            val icon = when (item) {
                is BottomNavItem.Home -> Icons.Default.Home
                is BottomNavItem.Build -> Icons.Default.Create
                is BottomNavItem.Saved -> Icons.Default.Favorite
                is BottomNavItem.Account -> Icons.Default.Person
            }

            NavigationBarItem(
                selected = navBackStackEntry?.destination?.route == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                },
                label = { Text(item.label) },
                icon = { Icon(icon, contentDescription = item.label) }
            )
        }
    }
}
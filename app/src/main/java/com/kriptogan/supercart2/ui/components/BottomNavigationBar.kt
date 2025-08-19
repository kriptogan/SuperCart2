package com.kriptogan.supercart2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation bar for the SuperCart app
 * @param currentRoute Current selected route
 * @param onRouteSelected Callback when a route is selected
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = {
                Text("Home")
            },
            selected = currentRoute == "home",
            onClick = { onRouteSelected("home") }
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Shopping List"
                )
            },
            label = {
                Text("Shopping List")
            },
            selected = currentRoute == "shopping_list",
            onClick = { onRouteSelected("shopping_list") }
        )
    }
}

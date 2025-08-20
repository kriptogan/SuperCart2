package com.example.supercart2.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.example.supercart2.R

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onRouteChange: (String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    
    val items = listOf(
        NavigationItem(
            route = "home",
            title = "Home",
            icon = R.drawable.ic_home
        ),
        NavigationItem(
            route = "shopping_list",
            title = "Shopping List",
            icon = R.drawable.ic_shopping_cart
        )
    )
    
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = item.icon), contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    selectedItem = index
                    onRouteChange(item.route)
                }
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: Int
)

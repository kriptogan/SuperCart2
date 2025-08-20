package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors

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
    
    NavigationBar(
        containerColor = SuperCartColors.primaryGreen,
        contentColor = SuperCartColors.white
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        painter = painterResource(id = item.icon), 
                        contentDescription = item.title,
                        modifier = Modifier.padding(4.dp)
                    ) 
                },
                label = { 
                    Text(
                        text = item.title,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    ) 
                },
                selected = currentRoute == item.route,
                onClick = {
                    selectedItem = index
                    onRouteChange(item.route)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SuperCartColors.white,
                    selectedTextColor = SuperCartColors.white,
                    unselectedIconColor = SuperCartColors.white.copy(alpha = 0.7f),
                    unselectedTextColor = SuperCartColors.white.copy(alpha = 0.7f),
                    indicatorColor = SuperCartColors.white.copy(alpha = 0.2f)
                )
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: Int
)

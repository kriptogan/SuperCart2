package com.example.supercart2.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onRouteChange: (String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = SuperCartColors.primaryGreen, contentColor = SuperCartColors.white,
    ) { BottomNavigationItems(currentRoute, onSelected = { selectedItem = it }, onRouteChange = onRouteChange) }
}

data class NavigationItem(
    val route: String,
    val titleRes: Int,
    val icon: Int
)

private val bottomNavigationItems = listOf(
    NavigationItem(route = "home", titleRes = R.string.nav_home, icon = R.drawable.ic_home),
    NavigationItem(route = "stores", titleRes = R.string.nav_stores, icon = R.drawable.ic_store),
)

/** One item per route; a click first records the item index, then reports its route. */
@Composable
private fun RowScope.BottomNavigationItems(
    currentRoute: String, onSelected: (Int) -> Unit, onRouteChange: (String) -> Unit,
) {
    bottomNavigationItems.forEachIndexed { index, item ->
        BottomNavigationItem(item, selected = currentRoute == item.route) {
            onSelected(index)
            onRouteChange(item.route)
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItem(item: NavigationItem, selected: Boolean, onClick: () -> Unit) {
    val title = stringResource(item.titleRes)
    NavigationBarItem(
        icon = { BottomNavigationIcon(item.icon, title) },
        label = { Text(text = title, style = MaterialTheme.typography.labelSmall) },
        selected = selected, onClick = onClick, colors = bottomNavigationItemColors(),
    )
}

@Composable
private fun BottomNavigationIcon(@DrawableRes icon: Int, description: String) {
    Icon(painter = painterResource(id = icon), contentDescription = description, modifier = Modifier.padding(4.dp))
}

@Composable
private fun bottomNavigationItemColors(): NavigationBarItemColors = NavigationBarItemDefaults.colors(
    selectedIconColor = SuperCartColors.white, selectedTextColor = SuperCartColors.white,
    unselectedIconColor = SuperCartColors.white.copy(alpha = 0.7f),
    unselectedTextColor = SuperCartColors.white.copy(alpha = 0.7f),
    indicatorColor = SuperCartColors.white.copy(alpha = 0.2f),
)

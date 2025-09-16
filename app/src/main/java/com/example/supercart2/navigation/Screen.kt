package com.example.supercart2.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ShoppingList : Screen("shopping_list")
}

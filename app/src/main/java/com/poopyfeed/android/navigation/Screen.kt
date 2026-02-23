package com.poopyfeed.android.navigation

sealed class Screen(val route: String) {
    data object Greeting : Screen("greeting")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
}

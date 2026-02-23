package com.poopyfeed.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poopyfeed.android.ui.screens.HomeScreen
import com.poopyfeed.android.ui.screens.children.ChildrenListScreen
import com.poopyfeed.android.ui.screens.greeting.GreetingScreen
import com.poopyfeed.android.ui.screens.login.LoginScreen
import com.poopyfeed.android.ui.screens.signup.SignupScreen
import com.poopyfeed.android.ui.screens.profile.ProfileScreen

@Composable
fun PoopyFeedNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Greeting.route,
        modifier = modifier,
    ) {
        composable(Screen.Greeting.route) {
            GreetingScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Greeting.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onSignupSuccess = {
                    navController.navigate(Screen.ChildrenList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ChildrenList.route) {
            ChildrenListScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToChildDashboard = { childId ->
                    navController.navigate(Screen.Home.route)
                },
                onNavigateToAddChild = {
                    // TODO: Navigate to add child screen when implemented
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Greeting.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteSuccess = {
                    navController.navigate(Screen.Greeting.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

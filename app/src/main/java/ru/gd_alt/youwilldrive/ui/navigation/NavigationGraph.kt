package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen

// Define route constants
object NavRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    // Add more routes as needed
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOGIN
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(navController = navController)
        }

        composable(NavRoutes.HOME) {
            // TODO: Create and add HomeScreen
        }

        // Add more destinations as needed
    }
}
package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.gd_alt.youwilldrive.ui.components.Calendar
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen

@Serializable
object Login
@Serializable
object CalendarRoute

@Composable
fun NavigationGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Login
    ) {
        composable<Login> {
            LoginScreen(navController = navController)
        }

        composable<CalendarRoute> {
            Calendar()
        }
    }
}
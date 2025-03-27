package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarScreen
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import ru.gd_alt.youwilldrive.ui.screens.Notifications.NotificationsScreen

@Serializable
object LoginRoute
@Serializable
object CalendarRoute
@Serializable
object NotificationsRoute

@Composable
fun NavigationGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = LoginRoute
    ) {
        composable<LoginRoute> {
            LoginScreen(navController = navController)
        }

        composable<CalendarRoute> {
            CalendarScreen()
        }

        composable<NotificationsRoute> {
            NotificationsScreen()
        }
    }
}
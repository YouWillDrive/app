package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarScreen
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import ru.gd_alt.youwilldrive.ui.screens.Notifications.NotificationsScreen

enum class NavRoutes(val value: String) {
    login("login"),
    calendar("calendar"),
    notifications("notifications"),
    profile("profile")
}

@Composable
fun NavigationGraph(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavRoutes.login.name
    ) {
        composable(NavRoutes.login.name) {
            LoginScreen(navController = navController)
        }

        composable(NavRoutes.calendar.name) {
            CalendarScreen()
        }

        composable(NavRoutes.notifications.name) {
            NotificationsScreen()
        }
    }
}
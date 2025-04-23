package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarScreen
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import ru.gd_alt.youwilldrive.ui.screens.Notifications.NotificationsScreen
import ru.gd_alt.youwilldrive.ui.screens.Profile.ProfileScreen

@Composable
fun NavigationGraph(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    var userId by remember { mutableStateOf("") }
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = LoginRoute
    ) {
        composable<LoginRoute> {
            LoginScreen(navController = navController) {
                userId = it
            }
        }

        composable<CalendarRoute> {
            CalendarScreen(userId)
        }

        composable<NotificationsRoute> {
            NotificationsScreen()
        }

        composable<ProfileRoute> {
            ProfileScreen(userId)
        }
    }
}
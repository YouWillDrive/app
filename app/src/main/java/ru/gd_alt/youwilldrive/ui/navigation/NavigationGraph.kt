package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.gd_alt.youwilldrive.ui.screens.CadetProfile.CadetProfileScreen
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarScreen
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import ru.gd_alt.youwilldrive.ui.screens.Notifications.NotificationsScreen

@Composable
fun NavigationGraph(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        modifier = modifier,
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

        composable<ProfileRoute> {
            CadetProfileScreen()
        }
    }
}
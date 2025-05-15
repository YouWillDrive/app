package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.gd_alt.youwilldrive.ui.screens.CadetsList.CadetsListScreen
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarScreen
import ru.gd_alt.youwilldrive.ui.screens.Chat.ChatScreen
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import ru.gd_alt.youwilldrive.ui.screens.Notifications.NotificationsScreen
import ru.gd_alt.youwilldrive.ui.screens.Profile.ProfileScreen

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Route
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Route.Login> {
            LoginScreen(navController = navController)
        }

        composable<Route.Calendar> {
            CalendarScreen()
        }

        composable<Route.Notifications> {
            NotificationsScreen()
        }

        composable<Route.Profile> {
            ProfileScreen(navController = navController)
        }

        composable<Route.CadetsList> {
            CadetsListScreen(navController = navController)
        }

        composable<Route.Chat> {
            ChatScreen()
        }
    }
}
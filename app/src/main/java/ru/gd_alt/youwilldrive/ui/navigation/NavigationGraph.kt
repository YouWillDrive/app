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
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarScreen
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import ru.gd_alt.youwilldrive.ui.screens.Notifications.NotificationsScreen
import ru.gd_alt.youwilldrive.ui.screens.Profile.ProfileScreen

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Route,
    dataStoreManager: DataStoreManager
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Route.Login> {
            LoginScreen(navController = navController, onSuccessfulLogin = { userId ->
                navController.navigate(Route.Calendar) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            })
        }

        composable<Route.Calendar> {
            CalendarScreen()
        }

        composable<Route.Notifications> {
            NotificationsScreen()
        }

        composable<Route.Profile> {
            ProfileScreen()
        }
    }
}
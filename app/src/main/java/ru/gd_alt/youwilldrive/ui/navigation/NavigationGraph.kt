package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import ru.gd_alt.youwilldrive.ui.components.Calendar
import ru.gd_alt.youwilldrive.ui.components.MonthSelector
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginScreen
import java.time.LocalDate

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
            var month by remember { mutableIntStateOf(LocalDate.now().monthValue) }
            var year by remember { mutableIntStateOf(LocalDate.now().year) }
            Column {
                MonthSelector(
                    defaultMonth = month,
                    defaultYear = year,
                    onMonthYearChanged = {
                        m, y -> month = m; year = y
                    }
                )
                Calendar(month = month, year = year)
            }
        }
    }
}
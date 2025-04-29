package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.gd_alt.youwilldrive.ui.navigation.Route

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    currentRoute: Route?,
    topLevelRoutes: List<Route> = Route.topLevelRoutes
) {
    NavigationBar(modifier) {
        topLevelRoutes.forEach { route ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = route.imageVector ?: Icons.Default.Circle,
                        contentDescription = stringResource(route.titleId),
                        Modifier.size(24.dp)
                    )
                },
                label = { Text(stringResource(route.titleId)) },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    val navController = rememberNavController()
    // Simulate state for the preview
    // Remember mutable state for the preview to potentially see selection changes (optional)
    var currentRoutePreview by remember { mutableStateOf<Route>(Route.Calendar) }

    MaterialTheme {
        BottomNavBar(
            navController = navController,
            currentRoute = currentRoutePreview, // Provide a simulated current route
            // topLevelRoutes uses the default from the Route companion object
            modifier = Modifier
        )
    }
}
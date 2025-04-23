package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.gd_alt.youwilldrive.ui.navigation.LoginRoute
import ru.gd_alt.youwilldrive.ui.navigation.NavRoutes
import ru.gd_alt.youwilldrive.ui.navigation.Route

@Preview
@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    topLevelRoutes: List<Route> = NavRoutes.filter { it.isTopLevel() }
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember { derivedStateOf { currentBackStackEntry?.destination?.route ?: LoginRoute } }
    if (topLevelRoutes.all { it.route::class.qualifiedName != currentRoute }) return

    NavigationBar(modifier) {
        topLevelRoutes.forEach { route ->
            NavigationBarItem(
                icon = {
                    Icon(
                        route.imageVector ?: Icons.Default.Circle,
                        contentDescription = stringResource(route.titleId),
                        Modifier.size(24.dp)
                    )
                },
                label = { Text(stringResource(route.titleId)) },
                selected = currentRoute == route.route::class.qualifiedName,
                onClick = {
                    navController.navigate(route.route)
                }
            )
        }
    }
}
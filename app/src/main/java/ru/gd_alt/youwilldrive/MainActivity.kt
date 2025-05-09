package ru.gd_alt.youwilldrive

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.components.BottomNavBar
import ru.gd_alt.youwilldrive.ui.navigation.NavigationGraph
import ru.gd_alt.youwilldrive.ui.navigation.Route

fun findRoute(routeId: Any?): Route? {
    val routeString = routeId as? String ?: return null

    return when (routeString) {
        // Assuming NavHost uses qualified names for objects by default with type-safe nav
        Route.Login::class.qualifiedName -> Route.Login
        Route.Calendar::class.qualifiedName -> Route.Calendar
        Route.Profile::class.qualifiedName -> Route.Profile
        Route.Notifications::class.qualifiedName -> Route.Notifications
        else -> {
            Log.w("MainActivity", "Could not find Route object for routeId: $routeString")
            Route.Login
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    @SuppressLint("RestrictedApi", "DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val applicationScope = CoroutineScope(SupervisorJob())
        Log.d("onCreate", "initd")
        Connection.initialize(this, applicationScope)

        enableEdgeToEdge()
        setContent {
            val dataStoreManager = DataStoreManager(this)
            val navController = rememberNavController()

            var startDestination by remember { mutableStateOf<Route>(Route.Login) }
            var isLoading by remember { mutableStateOf(true) }
            var user: User? by remember { mutableStateOf(null) }

            LaunchedEffect(key1 = dataStoreManager) {
                val userId = dataStoreManager.getUserId().first()
                Log.d("MainActivity", "User ID: $userId")
                startDestination = if (!userId.isNullOrEmpty()) {
                    Route.Profile
                } else {
                    Route.Login
                }
                user = User.fromId(userId ?: "")
                isLoading = false
            }

            Log.d("MainActivity", "Start destination: $startDestination")

            val currentBackStackEntry by navController.currentBackStackEntryAsState()

            val currentRouteObject: Route? = remember(currentBackStackEntry) {
                currentBackStackEntry?.destination?.route?.let { routeId ->
                    findRoute(routeId)
                }
            }

            MaterialTheme {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else {
                    Scaffold(
                        topBar = topBar@{
                            if (currentRouteObject != null && currentRouteObject !is Route.Login) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            stringResource(currentRouteObject.titleId),
                                            fontWeight = FontWeight.Bold)
                                    },
                                    modifier = Modifier.clip(RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp)),
                                    actions = {
                                        IconButton({
                                            navController.popBackStack()
                                            navController.navigate(currentRouteObject)
                                        }) {
                                            Icon(
                                                Icons.Outlined.Refresh,
                                                "Refresh", // TODO
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.primary,
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (currentRouteObject?.isTopLevel() == true) {
                                BottomNavBar(
                                    navController = navController,
                                    topLevelRoutes = Route.topLevelRoutes,
                                    currentRoute = currentRouteObject,
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavigationGraph(Modifier.padding(innerPadding), navController, startDestination)
                    }
                }
            }
        }
    }
}
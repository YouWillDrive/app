package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import ru.gd_alt.youwilldrive.R

data class Route(
    val route: Any,
    val titleId: Int,
    val imageVector: ImageVector? = null
) {
    fun isTopLevel(): Boolean {
        return imageVector != null
    }
}


@Serializable
object LoginRoute
@Serializable
object CalendarRoute
@Serializable
object ProfileRoute
@Serializable
object NotificationsRoute

val NavRoutes = listOf(
    Route(LoginRoute, R.string.login),
    Route(CalendarRoute, R.string.calendar, Icons.Default.CalendarMonth),
    Route(ProfileRoute, R.string.profile, Icons.Default.Person),
    Route(NotificationsRoute, R.string.notifications, Icons.Default.Notifications),
)

fun navRouteByRoute(route: Any): Route? {
    for (r in NavRoutes) {
        if (r.route::class.qualifiedName == route) {
            return r
        }
    }
    return null
}

package ru.gd_alt.youwilldrive.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import ru.gd_alt.youwilldrive.R

@Serializable
sealed class Route {
    abstract val titleId: Int
    open val iconKey: String? = null

    fun isTopLevel(): Boolean = iconKey != null

    open val imageVector: ImageVector?
        get() = getIcon(iconKey)

    @Serializable
    object Login : Route() {
        override val titleId: Int = R.string.login
    }

    @Serializable
    object Calendar : Route() {
        override val titleId: Int = R.string.calendar
        override val iconKey: String = "calendar"
    }

    @Serializable
    object Profile : Route() {
        override val titleId: Int = R.string.profile
        override val iconKey: String = "person"
    }

    @Serializable
    object CadetsList : Route() {
        override val titleId: Int = R.string.cadets
    }

    @Serializable
    object Chat : Route() {
        override val titleId: Int = R.string.chat
    }

    @Serializable
    object Notifications : Route() {
        override val titleId: Int = R.string.notifications
        override val iconKey: String = "notifications"
    }

    companion object {
        fun getIcon(iconKey: String?): ImageVector? {
            return when (iconKey) {
                "calendar" -> Icons.Default.CalendarMonth
                "person" -> Icons.Default.Person
                "notifications" -> Icons.Default.Notifications
                else -> null
            }
        }

        val topLevelRoutes: List<Route> = listOf(
            Calendar, Profile, Notifications
        )
    }
}
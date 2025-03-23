package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser1

@Preview
@Composable
fun Notifications(
    notifications: List<Notification> = listOf(
        Notification(
            1, DefaultUser1,
            mapOf(Pair("1", "2")),
            2,
            true, true
        ),
        Notification(
            2, DefaultUser1,
            mapOf(Pair("1", "2")),
            1,
            true, true
        ),
        Notification(
            3, DefaultUser1,
            mapOf(Pair("1", "2")),
            3,
            true, true
        ),
)) {
    Column {
        notifications.forEach {
            Row {
                Text("${it.id}")
            }
        }
    }
}
package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser1

@Preview
@Composable
fun Notifications(
    notifications: List<Notification> = listOf(
        Notification(
            123123123, DefaultUser1,
            mapOf(Pair("title", "Title 1"), Pair("message", "Message 1"),),
            29032025,
            true, true
        ),
        Notification(
            234234234, DefaultUser1,
            mapOf(Pair("title", "Title 2"), Pair("message", "Message 2"),),
            13022025,
            true, true
        ),
        Notification(
            345345345, DefaultUser1,
            mapOf(Pair("title", "Title 3"), Pair("message", "Message 3"),),
            30032025,
            true, true
        ),
)) {
    Column(
        Modifier.fillMaxWidth()
            .padding(4.dp)
            .background(Color.White)
    ) {
        notifications.sortedBy { -it.dateSent }.forEach {
            Card (
                modifier = Modifier
                    .padding(6.dp, 4.dp),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Row (
                    Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(Modifier.size(50.dp).clip(CircleShape).background(Color.Red))
                    Column (
                        Modifier.weight(0.8f).padding(5.dp)
                    ) {
                        Text(
                            "${it.body["title"]}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${it.body["message"]}"
                        )
                    }
                    Text(
                        "${it.dateSent}",
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}
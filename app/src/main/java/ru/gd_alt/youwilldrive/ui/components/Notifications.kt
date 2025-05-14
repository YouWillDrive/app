package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.Notification
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Preview
@Composable
fun Notifications(
    modifier: Modifier = Modifier,
    notifications: List<Notification?>? = listOf(
        Notification(
            "123123123",
            "Вам назначен экзамен в ГИБДД",
            "Приходите на ул. Пушкина, д. Колотушкина завтра в 11:00",
            listOf(),
            LocalDateTime.now(),
            true, true
        ),
        Notification(
            "234234234",
            "Перенести урок",
            "Ваш инструктор предложил перенести занятие на 13:00 30 февраля.",
            listOf(),
            LocalDateTime.now().plus(1, ChronoUnit.DAYS),
            true, true
        ),
        Notification(
            "345345345",
            "А вы знали, что…",
            "…чем больше вы спите за рулем, тем больше у вас сил для вождения.",
            listOf(),
            LocalDateTime.now().minus(1, ChronoUnit.DAYS),
            false, false
        ),
    )
) {
    if (notifications.isNullOrEmpty()) {
        EmptyNotificationsView()
    } else {
        NotificationsList(notifications = notifications.filter { it != null }.sortedByDescending { it!!.dateSent })
    }
}

@Composable
private fun EmptyNotificationsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_notifications_yet),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun NotificationsList(notifications: List<Notification?>) {
    val verticalArrangement = if (notifications.isEmpty()) {
        Arrangement.Center
    } else {
        Arrangement.spacedBy(8.dp)
    }

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        verticalArrangement = verticalArrangement
    ) {
        items(notifications) { notification ->
            NotificationItem(notification = notification as Notification)
        }
    }
}

@Composable
private fun NotificationItem(notification: Notification) {
    val dateTime = notification.dateSent
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    val formattedDate = formatDate(dateTime)

    val backgroundColor = if (!notification.read) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (!notification.read) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(0.8f)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (!notification.read) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun formatDate(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()

    return when {
        dateTime.toLocalDate() == now.toLocalDate() -> {
            // Today, show time
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        dateTime.toLocalDate() == now.minusDays(1).toLocalDate() -> {
            // Yesterday
            stringResource(R.string.yesterday) + "\n" +
                    dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        dateTime.year == now.year -> {
            dateTime.format(DateTimeFormatter.ofPattern("d.MM")) + "\n" +
                    dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        else -> {
            dateTime.format(DateTimeFormatter.ofPattern("d MM yyyy"))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsPreview() {
    Notifications()
}

@Preview(showBackground = true)
@Composable
fun EmptyNotificationsPreview() {
    Notifications(notifications = emptyList())
}
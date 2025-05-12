package ru.gd_alt.youwilldrive.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.toJavaLocalDateTime
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.EventType
import ru.gd_alt.youwilldrive.models.Instructor
import ru.gd_alt.youwilldrive.models.Placeholders
import ru.gd_alt.youwilldrive.models.Role
import ru.gd_alt.youwilldrive.models.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun EventDisplay(
    modifier: Modifier = Modifier,
    events: List<Event> = emptyList(),
    myRole: Role,
    onAddEvent: () -> Unit = {},
    date: LocalDate = LocalDate.now(),
    onEventSelection: (Event) -> Unit = {},
) {
    val monthNames = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.events) + " ${date.dayOfMonth} ${monthNames[date.monthValue - 1]} ${date.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                EmptyEventsView(date = date)
            } else {
                EventsList(events = events, myRole = myRole, onEventSelection = onEventSelection)
            }

            if (date >= LocalDate.now().plus(1L, ChronoUnit.DAYS) && myRole.name == "Инструктор") {
                EventAddItemButton(onAddEvent)
            }
        }
    }
}

@Composable
private fun EmptyEventsView(
    date: LocalDate = LocalDate.now()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.InsertEmoticon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.no_events_planned),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun EventsList(events: List<Event>, myRole: Role, onEventSelection: (Event) -> Unit = {}) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(events) { event ->
            EventItem(event, myRole, onEventSelection)
        }
    }
}

@Composable
private fun EventItem(event: Event, myRole: Role, onClick: (Event) -> Unit = {}) {
    val dateTime = event.date.toJavaLocalDateTime()

    val eventTypeColors = mutableMapOf<String, Color>(
        "event_types:lesson" to Color(0xFF39A0ED),
        "event_types:sai_exam" to Color(0xFF04724D),
        "event_types:sai_lesson" to Color(0xFF950952),
        "event_types:school_exam" to Color(0xFFD1D646),
    )

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val formattedTime = dateTime.format(timeFormatter)
    var eventType by remember { mutableStateOf<EventType?>(null) }
    var eventCadet by remember { mutableStateOf<Cadet?>(null) }
    var eventInstructor by remember { mutableStateOf<Instructor?>(null) }
    var displayParticipant by remember { mutableStateOf<User?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(event) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var eventColor by remember(event) { mutableStateOf<Color?>(null) }

            LaunchedEffect(event) {
                try {
                    val type = event.eventType()
                    eventColor = eventTypeColors[type!!.id] ?: Color.Gray
                } catch (e: Exception) {
                    Log.e("CalendarDay", "Error fetching event type for event ${event.id}: ${e.message}")
                    eventColor = Color.Gray
                }
            }

            LaunchedEffect(event) {
                try {
                    eventType = event.eventType()
                    eventCadet = event.ofCadet()
                    eventInstructor = event.ofInstructor()
                    displayParticipant = (if (myRole.name == "Курсант") eventInstructor else eventCadet)!!.me()
                } catch (e: Exception) {
                    Log.e("CalendarDay", "Error fetching event type for event ${event.id}: ${e.message}")
                    eventType = null
                }
            }

            // Event type indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = eventColor ?: Color.Gray
            ) {}

            Spacer(modifier = Modifier.width(16.dp))

            // Event info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = eventType?.name ?: "XXX",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${displayParticipant?.name} ${displayParticipant?.patronymic} ${displayParticipant?.surname}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview
@Composable
private fun EventAddItemButton(onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))

            IconButton(
                onClick,
                Modifier.weight(1f),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    stringResource(R.string.addEventButton)
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventDisplayPreview() {
    EventDisplay(events = Placeholders.DefaultEventList, myRole = Role("x", "Cadet"))
}

@Preview(showBackground = true)
@Composable
fun EmptyEventDisplayPreview() {
    EventDisplay(events = emptyList(), myRole = Role("x", "Cadet"))
}
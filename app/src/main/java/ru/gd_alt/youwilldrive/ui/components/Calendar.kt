package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.EventType
import java.time.YearMonth
import kotlin.random.Random
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun Calendar(
    modifier: Modifier = Modifier,
    month: Int = 12,
    year: Int = 2024,
    events: List<Event> = emptyList(),
    onDayClick: (Int) -> Unit = {}  // New parameter for day click callback
) {
    val yearMonth = YearMonth.of(year, month)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek.value % 7
    val eventsByDay = events.groupBy { it.id % daysInMonth + 1 } // Temporary mapping for example

    // Map of event types to consistent colors
    val eventTypeColors = events
        .map { it.type }
        .distinctBy { it.id }
        .associateWith {
            Color(
                red = Random(it.id).nextFloat() * 0.7f + 0.3f,
                green = Random(it.id * 2).nextFloat() * 0.7f + 0.3f,
                blue = Random(it.id * 3).nextFloat() * 0.7f + 0.3f
            )
        }

    Column(modifier = modifier.fillMaxWidth()) {
        // Calendar header with weekday names
        CalendarHeader()

        // Calendar days grid
        var dayCounter = 1
        val totalSlots = (daysInMonth + firstDayOfMonth + 6) / 7 * 7

        for (row in 0 until (totalSlots / 7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col
                    if (dayIndex < firstDayOfMonth || dayCounter > daysInMonth) {
                        // Empty cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                        )
                    } else {
                        // Day cell
                        val currentDay = dayCounter
                        val dayEvents = eventsByDay[currentDay] ?: emptyList()

                        CalendarDay(
                            day = currentDay,
                            events = dayEvents,
                            eventTypeColors = eventTypeColors,
                            onClick = { onDayClick(currentDay) }  // Pass click handler
                        )

                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    modifier: Modifier = Modifier,
    day: Int,
    events: List<Event>,
    eventTypeColors: Map<EventType, Color>,
    onClick: () -> Unit = {},  // New parameter for click handling
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            // Add clickable modifier to handle touches
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Day number
        Text(
            text = day.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
        )

        // Events indicators
        if (events.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                events.take(3).forEach { event ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(eventTypeColors[event.type] ?: MaterialTheme.colorScheme.primary)
                    )
                }

                // Show "+" indicator if there are more than 3 events
                if (events.size > 3) {
                    Text(
                        text = "+",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = if (day == "Сб" || day == "Вс") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
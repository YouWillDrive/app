package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.EventType
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

@Composable
@Preview
fun Calendar(
    modifier: Modifier = Modifier,
    month: Int = 10,
    year: Int = 2024,
    events: List<Event> = emptyList(),
    onDayClick: (Int) -> Unit = {
        day -> val selectedDate = "$day.$month.$year"
        android.util.Log.d("Calendar", "Selected date: $selectedDate")
    }
) {
    val yearMonth = YearMonth.of(year, month)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek.value % 7 - 1

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

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Calendar header with weekday names
            CalendarHeader()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                // Calendar days grid
                var dayCounter = 1

                Column(modifier = Modifier.padding(8.dp)) {
                    for (row in 0 until (daysInMonth / 7) + 1) {
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
                                    val dayEvents: List<Event> = events.filter {
                                        java.time.Instant.ofEpochSecond(it.date.toLong())
                                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                                            .dayOfMonth == currentDay
                                    }

                                    CalendarDay(
                                        modifier = Modifier,
                                        day = currentDay,
                                        events = dayEvents,
                                        eventTypeColors = eventTypeColors,
                                        onDayClick
                                    )

                                    dayCounter++
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDay(
    modifier: Modifier = Modifier,
    day: Int,
    events: List<Event>,
    eventTypeColors: Map<EventType, Color>,
    onClick: (Int) -> Unit
) {
    Card(
        modifier = modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick(day) }
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (events.isNotEmpty())
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (events.isNotEmpty()) 2.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Day number
            Text(
                text = day.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
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
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    eventTypeColors[event.type] ?: MaterialTheme.colorScheme.primary
                                )
                        )
                    }

                    if (events.size > 3) {
                        Text(
                            text = "+",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf(stringResource(R.string.mon),
                stringResource(R.string.tue), stringResource(R.string.wed),
                stringResource(R.string.thu), stringResource(R.string.fri),
                stringResource(R.string.sat), stringResource(R.string.sun)
            )
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day == stringResource(R.string.sat) || day == stringResource(R.string.sun))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
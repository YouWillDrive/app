package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Placeholders
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CalendarEventSystem(
    modifier: Modifier = Modifier,
    events: List<Event> = Placeholders.DefaultEventList,
    initialMonth: Int = LocalDate.now().monthValue,
    initialYear: Int = LocalDate.now().year
) {
    var currentMonth by remember { mutableIntStateOf(initialMonth) }
    var currentYear by remember { mutableIntStateOf(initialYear) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // Filter events for the selected date
    val displayedEvents = remember(selectedDay, currentMonth, currentYear, events) {
        if (selectedDay == null) {
            emptyList()
        } else {
            events.filter { event ->
                val eventDateTime = Instant
                    .ofEpochSecond(event.date.toLong())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()

                eventDateTime.dayOfMonth == selectedDay &&
                        eventDateTime.monthValue == currentMonth &&
                        eventDateTime.year == currentYear
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Month selector at the top
        MonthSelector(
            modifier = Modifier.padding(horizontal = 8.dp),
            defaultMonth = currentMonth,
            defaultYear = currentYear,
            onMonthYearChanged = { month, year ->
                currentMonth = month
                currentYear = year
                selectedDay = null // Reset selected day when changing month
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar in the middle
        Calendar(
            month = currentMonth,
            year = currentYear,
            events = events,
            onDayClick = { day ->
                selectedDay = day
            }
        )

        // Events display at the bottom, stretching to fill remaining space
        EventDisplay(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            events = displayedEvents
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarEventSystemPreview() {
    CalendarEventSystem()
}
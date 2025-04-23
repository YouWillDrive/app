package ru.gd_alt.youwilldrive.ui.screens.Calendar

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.toJavaLocalDateTime
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultUser
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.components.Calendar
import ru.gd_alt.youwilldrive.ui.components.EventDisplay
import ru.gd_alt.youwilldrive.ui.components.MonthSelector
import java.time.LocalDate

@Composable
fun CalendarScreen(
    userId: String,
    viewModel: CalendarViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var currentMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var currentYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var events: List<Event>? by remember { mutableStateOf(null) }

    LaunchedEffect(scope) {
        val user = (User.fromId(userId) ?: DefaultUser)
        Log.d("ProfileScreen", user.id)
        viewModel.fetchEvents(userId) { data, _ ->
            events = data
        }
        Log.d("CalendarScreen", "${events}")
    }
    Log.d("CalendarScreen", "outside ${events}")

    // Filter events for the selected date
    val displayedEvents = remember(selectedDay, currentMonth, currentYear, events) {
        if (selectedDay == null) {
            emptyList()
        } else {
            events?.filter { event ->
                val eventDateTime = event.date.toJavaLocalDateTime()

                eventDateTime.dayOfMonth == selectedDay &&
                        eventDateTime.monthValue == currentMonth &&
                        eventDateTime.year == currentYear
            } ?: emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
    CalendarScreen(DefaultUser.id)
}
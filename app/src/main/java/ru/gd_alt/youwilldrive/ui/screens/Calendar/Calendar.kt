package ru.gd_alt.youwilldrive.ui.screens.Calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toJavaLocalDateTime
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Role
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.components.Calendar
import ru.gd_alt.youwilldrive.ui.components.EventDisplay
import ru.gd_alt.youwilldrive.ui.components.MonthSelector
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CalendarScreen(
) {
    val scope = rememberCoroutineScope()
    var currentMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var currentYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var events: List<Event>? by remember { mutableStateOf(null) }
    var selectedEvent: Event? by remember { mutableStateOf(null) }

    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val factory = remember(dataStoreManager) {
        CalendarViewModelFactory(dataStoreManager)
    }

    val viewModel: CalendarViewModel = viewModel(factory = factory)
    val fetchState by viewModel.calendarState.collectAsState()

    LaunchedEffect(scope) {
        viewModel.fetchEvents()
    }
    events = viewModel.events.collectAsState().value

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

    if (viewModel.calendarState.collectAsState().value == CalendarState.Loading) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round,
                strokeWidth = 10.dp
            )
        }
        return
    }

    if (selectedEvent != null) {
        val onDismiss = {selectedEvent = null}
        BasicAlertDialog(onDismiss) {
            Card {
                Column(Modifier.padding(20.dp)) {
                    Text("Подтвердить событие?")
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Отмена",
                            Modifier.weight(0.5f).clickable { onDismiss() },
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Перенести",
                            Modifier.weight(0.5f).clickable { onDismiss() },
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Да",
                            Modifier.weight(0.5f).clickable { onDismiss() },
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
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

        var myRole by remember { mutableStateOf<Role?>(null) }

        LaunchedEffect(Unit) {
            myRole = User.fromId(dataStoreManager.getUserId().first()!!)!!.role()
        }

        // Events display at the bottom, stretching to fill remaining space
        EventDisplay(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            events = displayedEvents,
            myRole = myRole ?: Role("x", "Кадет")
        ) {
            selectedEvent = it
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarEventSystemPreview() {
    CalendarScreen()
}
package ru.gd_alt.youwilldrive.ui.screens.Calendar

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Placeholders
import ru.gd_alt.youwilldrive.models.Role
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.components.Calendar
import ru.gd_alt.youwilldrive.ui.components.EventDisplay
import ru.gd_alt.youwilldrive.ui.components.MonthSelector
import ru.gd_alt.youwilldrive.ui.navigation.Route
import ru.gd_alt.youwilldrive.ui.screens.EventEdit.EventEditDialog
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CalendarScreen(
    navController: NavController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    var currentMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var currentYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var events: List<Event>? by remember { mutableStateOf(null) }

    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val factory = remember(dataStoreManager) {
        CalendarViewModelFactory(dataStoreManager)
    }
    val eventEditOpen = remember { mutableStateOf(false) }

    var selectedEvent: Event? by remember { mutableStateOf(null) }
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
            myRole = User.fromId(dataStoreManager.getUserId().firstOrNull().toString())?.role()
        }

        // Events display at the bottom, stretching to fill remaining space
        EventDisplay(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            events = displayedEvents,
            myRole = myRole ?: Role("x", "Кадет"),
            onAddEvent = { eventEditOpen.value = true },
            date = LocalDate.of(currentYear, currentMonth, selectedDay ?: 1),
        ) {
            selectedEvent = it
        }

        Button(
            { navController.navigate(Route.Events) },
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                hoveredElevation = 3.dp,
                pressedElevation = 2.dp,
            )
        ) {
            Text("Все события")
        }

        Log.d("CalendarScreen", "Selected date: ${LocalDate.of(currentYear, currentMonth, selectedDay ?: 1)}")

        EventEditDialog(
            eventEditOpen,
            LocalDateTime(
                currentYear, currentMonth, (selectedDay ?: 1),
                12, 0, 0
            )
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        )
    }
}

@Composable
fun ConfirmPastEvent(event: Event?, onMainDismiss: () -> Unit = {}, onConfirm: (Long) -> Unit) {
    var duration by remember { mutableStateOf("1") };

    if (event == null) {
        return
    }

    Log.d("ConfirmPastEvent", event.toString())

    AlertDialog(
        onDismissRequest = onMainDismiss,
        title = {
            Text(
                "Подтверждение длительности события " + event.date.date.let {
                    "${it.dayOfMonth}.${it.monthNumber}.${it.year}"
                }
            )
        },
        text = {
            Column {
                Row(
                    Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.duration),
                        Modifier
                            .weight(3f),
                        style = MaterialTheme.typography.bodyLarge // Use typography
                    )
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            duration,
                            { duration = it },
                            Modifier.weight(1f),
                            textStyle = TextStyle(textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                it()
                                Spacer(
                                    Modifier
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .fillMaxWidth()
                                )
                            }
                        }
                        Text("ч.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        onConfirm(duration.toLong())
                        onMainDismiss()
                    }
                    catch (e: Exception) {
                        // TODO: Add dialog
                    }
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onMainDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUpcomingEvent(event: Event?, onDismiss: () -> Unit, onCancel: (Event) -> Unit, onPostpone: (Event, Long) -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
    )
    val timePickerState = rememberTimePickerState()
    var timePickerOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    if (event == null || event.date.toJavaLocalDateTime()
            .isBefore(java.time.LocalDateTime.now().plusDays(1))) return

    var isSameUser: MutableState<Boolean?> = remember { mutableStateOf(null) }
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }

    LaunchedEffect(event.id) {
        val userId = dataStoreManager.getUserId().first()
        if (userId == null) {
            isSameUser.value
            return@LaunchedEffect
        }
        if (
            (event.actualConfirmationValue("confirmation_types:to_happen") == -1L)
            && User.fromId(userId)?.isInstructor() != null
        ) {
            Log.d("isUser", "same")
            isSameUser.value = true
            return@LaunchedEffect
        }

        else {
            try {
                isSameUser.value = (event.confirmations().sortedBy { it.date }.last {
                    it.confirmationType()?.id == "confirmation_types:postpone"
                }.confirmator()?.id ?: userId) == userId
                Log.d("isUser", "same1")
                return@LaunchedEffect
            } catch (_: NoSuchElementException) {}
        }

        Log.d("isUser", "diff")
        isSameUser.value = false
    }

    Log.d("isUser", "checking user")
    if (isSameUser.value != false) return

    BasicAlertDialog(onDismiss) {
        Card {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                event.let {
                    Text("Перенести событие в ${it.date.date.dayOfMonth}.${it.date.date.monthNumber}.${it.date.date.year}?")
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        Modifier
                            .weight(0.5f)
                            .clickable { onDismiss() },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.cancel_event),
                        Modifier
                            .weight(0.5f)
                            .clickable {
                                onCancel(event)
                                onDismiss()
                           },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.postpone),
                        Modifier
                            .weight(0.5f)
                            .clickable { datePickerOpen = true },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (datePickerOpen) {
        val onDpDismiss = { datePickerOpen = false }
        DatePickerDialog(
            onDismissRequest = onDpDismiss,
            confirmButton = {
                TextButton(onClick@{
                    if (datePickerState.selectedDateMillis == null) {
                        return@onClick
                    }
                    timePickerOpen = true
                    onDpDismiss()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDpDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(datePickerState)
        }
    }

    if (timePickerOpen) {
        val onTpDismiss = { timePickerOpen = false }
        DatePickerDialog(
            onDismissRequest = onTpDismiss,
            confirmButton = {
                TextButton({
                    onDismiss()

                    val calendar = java.util.Calendar.getInstance()
                    val date = Instant.fromEpochMilliseconds(datePickerState.selectedDateMillis?: 0).toLocalDateTime(TimeZone.currentSystemDefault())
                    calendar.set(java.util.Calendar.YEAR, date.year)
                    calendar.set(java.util.Calendar.MONTH, date.monthNumber)
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, date.dayOfMonth)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)

                    Log.d("EditUpcomingEvent", "${Instant.fromEpochMilliseconds(calendar.timeInMillis).toLocalDateTime(TimeZone.currentSystemDefault())}")
                    onPostpone(event, calendar.timeInMillis)

                    onTpDismiss()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onTpDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(timePickerState)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarEventSystemPreview() {
    CalendarScreen()
}
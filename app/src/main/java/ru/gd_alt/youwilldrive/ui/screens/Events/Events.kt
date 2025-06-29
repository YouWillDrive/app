package ru.gd_alt.youwilldrive.ui.screens.Events

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultEvent
import ru.gd_alt.youwilldrive.models.Role
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.components.EventItem
import ru.gd_alt.youwilldrive.ui.navigation.Route
import ru.gd_alt.youwilldrive.ui.screens.Calendar.ConfirmPastEvent
import ru.gd_alt.youwilldrive.ui.screens.Calendar.EditUpcomingEvent

@Preview
@Composable
fun EventsScreen(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val factory = remember(dataStoreManager) {
        EventsViewModelFactory(dataStoreManager)
    }
    val viewModel: EventsViewModel = viewModel(factory = factory)
    val scope = rememberCoroutineScope()

    var myRole: Role? by remember { mutableStateOf(null) }

    var selectedEvent: Event? by remember { mutableStateOf(null) }

    val tabs = listOf(
        stringResource(R.string.unconfirmed_short),
        stringResource(R.string.upcoming),
        stringResource(R.string.past))
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }

    LaunchedEffect(scope) {
        viewModel.fetchEvents()

        myRole = User.fromId(dataStoreManager.getUserId().firstOrNull().toString())?.role()
    }
    val events = viewModel.events.collectAsState().value ?: listOf(DefaultEvent)

    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
                        selectedEvent = null
                    },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events.filter { !it.confirmed && !it.declined }) { event ->
                            EventItem(event, myRole ?: Role("x", "Курсант")) {
                                selectedEvent = it
                            }
                        }
                    }

                    ConfirmEventDialog(
                        selectedEvent,
                        onDismiss = { selectedEvent = null },
                        onConfirm = {
                            val selectedId = selectedEvent?.id
                            if (selectedId != null) {
                                viewModel.acceptEvent(selectedId)
                            }

                            navController.popBackStack()
                            navController.navigate(Route.Events)
                        },
                        onPostpone = { e, v ->
                            viewModel.postpone(e, v)

                            navController.popBackStack()
                            navController.navigate(Route.Events)
                        }
                    )
                }
                1 -> {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            events.filter { it.date > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) && it.confirmed}
                        ) { event ->
                            EventItem(event, myRole ?: Role("x", "Cadet")) {
                                selectedEvent = it
                            }
                        }
                    }

                    EditUpcomingEvent(
                        selectedEvent,
                        onDismiss = {
                            selectedEvent = null
                        },
                        onCancel = {
                            viewModel.declineEvent(it.id)
                        },
                        onPostpone = viewModel::postpone
                    )
                }
                2 -> {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            events.filter { it.date < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
                        ) { event ->
                            EventItem(event, myRole ?: Role("x", "Cadet")) {
                                selectedEvent = it
                            }
                        }
                    }

                    val currentEvent = selectedEvent
                    if (currentEvent != null && !currentEvent.durationAccepted) {
                        if (currentEvent.durationAsked) {
                            ConfirmDurationDialog(
                                currentEvent,
                                {
                                    selectedEvent = null
                                },
                                {
                                    viewModel.acceptDuration(currentEvent.id)
                                },
                                {
                                    viewModel.declineDuration(currentEvent.id)
                                    viewModel.confirmDuration(currentEvent.id, it)
                                }
                            )
                        }
                        else {
                            ConfirmPastEvent(
                                currentEvent,
                                {
                                    selectedEvent = null
                                },
                                {
                                    viewModel.confirmDuration(currentEvent.id, it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmEventDialog(event: Event?, onDismiss: () -> Unit, onConfirm: (Int) -> Unit, onPostpone: (Event, Long) -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds()
    )
    val timePickerState = rememberTimePickerState()
    var timePickerOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    if (event == null || event.date.toJavaLocalDateTime().isBefore(java.time.LocalDateTime.now())) return
    var isSameUser: MutableState<Boolean?> = remember { mutableStateOf(null) }
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }

    LaunchedEffect(event.id) {
        val userId = dataStoreManager.getUserId().first()
        if (userId == null) {
            isSameUser.value = true
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
                }.confirmator()?.id ?: event.ofInstructor()?.id) == userId
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
                Text(stringResource(R.string.confirm_event_ask))

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
                        stringResource(R.string.postpone),
                        Modifier
                            .weight(0.5f)
                            .clickable { datePickerOpen = true },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.yes),
                        Modifier
                            .weight(0.5f)
                            .clickable { onConfirm(1); onDismiss() },
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
                TextButton({
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDurationDialog(event: Event?, onDismiss: () -> Unit, onConfirm: () -> Unit, onReconfirm: (Long) -> Unit) {
    if (event == null) return

    val isSameUser: MutableState<Boolean?> = remember { mutableStateOf(null) }
    val requiresConfirmation: MutableState<Boolean?> = remember { mutableStateOf(null) }
    var askedDuration: Long? by remember { mutableStateOf(null) }
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    var declineDurationOpen by remember { mutableStateOf(false) }

    LaunchedEffect(event.id) {
        requiresConfirmation.value = !event.durationAccepted
        askedDuration = event.actualConfirmationValue("confirmation_types:duration")
        val userId = dataStoreManager.getUserId().first()
        if (userId == null) {
            isSameUser.value = true
            return@LaunchedEffect
        }
        try {
            Log.d("isUser", "${event.confirmations().sortedBy { it.date }.last {
                it.confirmationType()?.id == "confirmation_types:duration"
            }.confirmator()?.id} ${userId} ${event.confirmations().sortedBy { it.date }.last {
                it.confirmationType()?.id == "confirmation_types:duration"
            }.confirmator()?.id == userId}")
            isSameUser.value = (event.confirmations().sortedBy { it.date }.last {
                it.confirmationType()?.id == "confirmation_types:duration"
            }.confirmator()?.id ?: userId) == userId
            Log.d("isUser", "same")
            return@LaunchedEffect
        } catch (_: NoSuchElementException) {}

        Log.d("isUser", "diff")
        isSameUser.value = false

    }

    Log.d("ConfirmDurationDialog", "checking requirements")
    Log.d("ConfirmDurationDialog", "${isSameUser.value} ${requiresConfirmation.value}")
    if (isSameUser.value == true || requiresConfirmation.value == false) return

    BasicAlertDialog(onDismiss) {
        Card {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Занятие длилось ${askedDuration ?: "..."} часов?")

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
                        stringResource(R.string.no),
                        Modifier
                            .weight(0.5f)
                            .clickable {
                                declineDurationOpen = true
                                Log.d("ConfirmDurationDialog", "clicked")
                            },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.yes),
                        Modifier
                            .weight(0.5f)
                            .clickable { onConfirm(); onDismiss() },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Log.d("ConfirmDurationDialog", "$declineDurationOpen $event")
    if (declineDurationOpen) {
        ConfirmPastEvent(
            event,
            onDismiss,
            onConfirm = {
                if (it == askedDuration) {
                    onConfirm()
                }
                else onReconfirm(it)
            }
        )
    }
}

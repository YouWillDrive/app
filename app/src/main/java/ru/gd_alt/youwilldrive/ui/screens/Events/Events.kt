package ru.gd_alt.youwilldrive.ui.screens.Events

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCadet
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultEvent
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultEvent1
import ru.gd_alt.youwilldrive.models.Role
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.components.EventItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen() {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    var events by remember { mutableStateOf(listOf(DefaultEvent, DefaultEvent1)) }
    var myRole: Role? by remember { mutableStateOf(null) }

    var selectedEvent: Event? by remember { mutableStateOf(null) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    var timePickerOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(scope) {
        val user = User.fromId(dataStoreManager.getUserId().first { !it.isNullOrEmpty() } ?: "")
        events = (
                (
                        user?.isCadet() ?: user?.isInstructor()
                        )?.events() ?: emptyList()
                ).fastFilter {
                true // TODO: !Event.confirmed()
                }

        myRole = User.fromId(dataStoreManager.getUserId().firstOrNull().toString())?.role()
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (event in events) {
                EventItem(event, myRole ?: Role("x", "Cadet")) {
                    selectedEvent = it
                }
            }

        }
    }

    if (selectedEvent != null) {
        val onDismiss = {selectedEvent = null} // TODO
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
                                .clickable { datePickerOpen = true },  // TODO
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            stringResource(R.string.yes),
                            Modifier
                                .weight(0.5f)
                                .clickable { onDismiss() },
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (datePickerOpen) {
        val onDismiss = { datePickerOpen = false }
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton({
                    timePickerOpen = true
                    onDismiss()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(datePickerState)
        }
    }

    if (timePickerOpen) {
        val onDismiss = { timePickerOpen = false }
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton({
                    selectedEvent = null
                    onDismiss() // TODO
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
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

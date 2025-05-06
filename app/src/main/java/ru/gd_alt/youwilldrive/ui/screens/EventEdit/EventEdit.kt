package ru.gd_alt.youwilldrive.ui.screens.EventEdit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.Event
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun EventEditDialog(
    dialogOpen: MutableState<Boolean> = remember { mutableStateOf(false) },
    event: Event? = null,
    onConfirm: (Event) -> Unit = {}
) {
    if (!dialogOpen.value) return

    // TODO: Bind event properties
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    var datePickerOpen by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }
    var cadetPickerOpen by remember { mutableStateOf(false) }

    val onMainDismiss = { dialogOpen.value = false }

    BasicAlertDialog(
        {
            // Should be closed by buttons
        }
    ) {
        Card {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { datePickerOpen = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.date))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${datePickerState.selectedDateMillis}")
                        Icon(
                            Icons.Default.ArrowDropDown,
                            stringResource(R.string.date)
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { timePickerOpen = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.time))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${timePickerState.hour}:${timePickerState.minute}")
                        Icon(
                            Icons.Default.ArrowDropDown,
                            stringResource(R.string.time)
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { cadetPickerOpen = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.cadet))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.cadet))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            stringResource(R.string.cadet)
                        )
                    }
                }

                Spacer(Modifier.size(20.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        Modifier
                            .weight(1f)
                            .clickable { onMainDismiss() },
                        MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.ok),
                        Modifier
                            .weight(1f)
                            .clickable {
                                onConfirm(
                                    Event.fromDictionary(
                                        mapOf(
                                            "id" to (event?.id ?: ""),
                                            "datetime" to Instant.ofEpochMilli(
                                                (datePickerState.selectedDateMillis ?: 0)
                                                        + (timePickerState.hour * 60 + timePickerState.minute)
                                                        * 60
                                            ).toKotlinInstant()
                                                .toLocalDateTime(TimeZone.currentSystemDefault()),
                                        )
                                    )
                                )
                                onMainDismiss()
                            },
                        MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (datePickerOpen) {

        val onDismiss = { datePickerOpen = false }
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton({ onDismiss(); /* TODO */ }) {
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
                TextButton({ onDismiss(); /* TODO */ }) {
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

    DropdownMenu(
        expanded = cadetPickerOpen,
        onDismissRequest = { cadetPickerOpen = false }
    ) {
        // TODO
        DropdownMenuItem(
            text = { Text("Кадет 1") },
            onClick = { cadetPickerOpen = false }
        )
        DropdownMenuItem(
            text = { Text("Кадет 2") },
            onClick = { cadetPickerOpen = false }
        )
    }
}
package ru.gd_alt.youwilldrive.ui.screens.EventEdit

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.R
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAmount
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun EventEditDialog(
    dialogOpen: MutableState<Boolean> = remember { mutableStateOf(false) },
    initialDateTimeMillis: Long = Instant.now().toEpochMilli(),
    onConfirm: (LocalDateTime /* TODO: Add Cadet ID/Object */) -> Unit = {},
    availableCadets: List<String> = listOf("Кадет 1", "Кадет 2", "Кадет 3")
) {
    if (!dialogOpen.value) return

    // TODO: Bind initial event properties (date, time, selected cadet) to state variables
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.ofEpochMilli(initialDateTimeMillis).plus(Duration.ofDays(1L)).toEpochMilli()
    )

    val initialDateTime = Instant.ofEpochMilli(initialDateTimeMillis).toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute
    )
    var duration by remember { mutableStateOf("123") };

    // State for controlling dialogs/menus visibility
    var datePickerOpen by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }
    var cadetPickerOpen by remember { mutableStateOf(false) }

    // State for selected cadet
    // TODO: Bind initial selected cadet
    var selectedCadet by remember { mutableStateOf<String?>(null) }

    val onMainDismiss = { dialogOpen.value = false }

    AlertDialog(
        onDismissRequest = onMainDismiss,
        title = {
            Text(
                stringResource(R.string.edit_event)
                + " " + kotlinx.datetime.Instant.fromEpochMilliseconds(
                    datePickerState.selectedDateMillis ?: Instant.now().toEpochMilli() // Handle null state
                ).toLocalDateTime(TimeZone.currentSystemDefault()).date.format(
                    LocalDate.Format {
                        dayOfMonth(); char('.'); monthNumber(); char('.'); year()
                    }
                )
            )
                },
        text = {
            Column {
                // Time Selection Row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { timePickerOpen = true }
                        .padding(vertical = 12.dp), // Add padding for better touch target
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.time), style = MaterialTheme.typography.bodyLarge) // Use typography
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${timePickerState.hour}".padStart(2, '0')
                                    + ":"
                                    + "${timePickerState.minute}".padStart(2, '0'),
                            style = MaterialTheme.typography.bodyMedium // Use typography for value
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_time), // Add content description
                            Modifier.size(24.dp) // Standard icon size
                        )
                    }
                }

                Row(
                    Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(), // Add padding for better touch target
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.duration),
                        Modifier
                            .weight(1f),
                        style = MaterialTheme.typography.bodyLarge // Use typography
                    )
                    BasicTextField(
                        duration,
                        { duration = it },
                        Modifier
                            .weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Cadet Selection Row/Dropdown
                Box { // Box is needed to anchor the DropdownMenu
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { cadetPickerOpen = true }
                            .padding(vertical = 12.dp), // Add padding for better touch target
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.cadet), style = MaterialTheme.typography.bodyLarge) // Use typography
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // TODO: Display selected cadet name
                            Text(
                                selectedCadet ?: stringResource(R.string.select_cadet), // Show hint if none selected
                                style = MaterialTheme.typography.bodyMedium // Use typography for value
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.select_cadet), // Add content description
                                Modifier.size(24.dp) // Standard icon size
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = cadetPickerOpen,
                        onDismissRequest = { cadetPickerOpen = false }
                    ) {
                        // TODO: Populate this list dynamically from availableCadets prop
                        availableCadets.forEach { cadetName ->
                            DropdownMenuItem(
                                text = { Text(cadetName) },
                                onClick = {
                                    selectedCadet = cadetName // TODO: Store/Use actual cadet ID/Object
                                    cadetPickerOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Calculate the selected LocalDateTime
                    val selectedDateMillis = datePickerState.selectedDateMillis ?: initialDateTimeMillis // Use initial or current if date picker was not opened
                    val selectedTimeMillis = TimeUnit.HOURS.toMillis(timePickerState.hour.toLong()) +
                            TimeUnit.MINUTES.toMillis(timePickerState.minute.toLong())

                    // Combine date and time. Be careful with time zones and milliseconds precision.
                    // A robust solution might involve converting dateMillis to a LocalDate first.
                    val selectedLocalDate = Instant.ofEpochMilli(selectedDateMillis)
                        .toKotlinInstant()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date

                    val selectedLocalDateTime = LocalDateTime(
                        year = selectedLocalDate.year,
                        month = selectedLocalDate.month,
                        dayOfMonth = selectedLocalDate.dayOfMonth,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        second = 0,
                        nanosecond = 0
                    )

                    // TODO: Pass selected cadet data along with LocalDateTime
                    onConfirm(selectedLocalDateTime /* TODO: Add selectedCadet */)
                    onMainDismiss()
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

    if (datePickerOpen) {
        val onDismiss = { datePickerOpen = false }
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton({
                    // TODO: The datePickerState is already updated when the user selects a date.
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
        DatePickerDialog( // Note: Using DatePickerDialog wrapper for TimePicker, which is okay.
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton({
                    // TODO: The timePickerState is already updated when the user confirms.
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
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(timePickerState)
            }
        }
    }
}
package ru.gd_alt.youwilldrive.ui.screens.EventEdit

import androidx.compose.foundation.background
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.data.DataStoreManager
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun EventEditDialog(
    dialogOpen: MutableState<Boolean> = remember { mutableStateOf(false) },
    initialDateTimeMillis: Long = Instant.now().toEpochMilli(),
    add: Boolean = true
) {
    if (!dialogOpen.value) return

    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val viewModel : EventEditViewModel = viewModel(
        factory = EventEditViewModelFactory(dataStoreManager)
    )


    val scope = rememberCoroutineScope()
    var cadets by remember { mutableStateOf(listOf("" to "")) }
    var types by remember { mutableStateOf(listOf("" to "")) }
    LaunchedEffect(scope) {
        viewModel.fetchCadetsIdName { data, _ ->
            cadets = data
        }
        viewModel.fetchEventTypes { data, _ ->
            types = data
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTimeMillis
    )

    val initialDateTime = Instant.ofEpochMilli(initialDateTimeMillis).toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute
    )
    var duration by remember { mutableStateOf("1") };

    // State for controlling dialogs/menus visibility
    var datePickerOpen by remember { mutableStateOf(false) }
    var timePickerOpen by remember { mutableStateOf(false) }
    var cadetPickerOpen by remember { mutableStateOf(false) }
    var typePickerOpen by remember { mutableStateOf(false) }

    // State for selected cadet and type
    var selectedCadet by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }

    val onMainDismiss = { dialogOpen.value = false }

    AlertDialog(
        onDismissRequest = onMainDismiss,
        title = {
            Text(
                stringResource(R.string.edit_event)
                + " " + kotlinx.datetime.Instant.fromEpochMilliseconds(
                    datePickerState.selectedDateMillis ?: Instant.now().toEpochMilli() // Handle null state
                ).toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
                    "${it.dayOfMonth}.${it.monthNumber}.${it.year}"
                }
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

//                Row(
//                    Modifier
//                        .padding(vertical = 12.dp)
//                        .fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        stringResource(R.string.duration),
//                        Modifier
//                            .weight(3f),
//                        style = MaterialTheme.typography.bodyLarge // Use typography
//                    )
//                    Row(
//                        Modifier.weight(1f),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        BasicTextField(
//                            duration,
//                            { duration = it },
//                            Modifier.weight(1f),
//                            textStyle = TextStyle(textAlign = TextAlign.Center),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                            singleLine = true
//                        ) {
//                            Column(
//                                horizontalAlignment = Alignment.CenterHorizontally
//                            ) {
//                                it()
//                                Spacer(
//                                    Modifier
//                                        .height(1.dp)
//                                        .background(MaterialTheme.colorScheme.primary)
//                                        .fillMaxWidth()
//                                )
//                            }
//                        }
//                        Text("ч.")
//                    }
//                }

                // Type Selection Row/Dropdown
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { typePickerOpen = true }
                            .padding(vertical = 12.dp), // Add padding for better touch target
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.type), style = MaterialTheme.typography.bodyLarge) // Use typography
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // TODO: Display selected type name
                            Text(
                                selectedType ?: stringResource(R.string.select_type), // Show hint if none selected
                                style = MaterialTheme.typography.bodyMedium // Use typography for value
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.select_type), // Add content description
                                Modifier.size(24.dp) // Standard icon size
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = typePickerOpen,
                        onDismissRequest = { typePickerOpen = false }
                    ) {
                        types.forEach {
                            DropdownMenuItem(
                                text = { Text(it.second) },
                                onClick = {
                                    selectedType = it.first
                                    typePickerOpen = false
                                }
                            )
                        }
                    }
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
                        cadets.forEach { cadet ->
                            DropdownMenuItem(
                                text = { Text(cadet.second) },
                                onClick = {
                                    selectedCadet = cadet.first
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
                onClick = onClick@{
                    // TODO: Add warning if not selected
                    if (selectedType == null || selectedCadet == null) return@onClick
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
                    val cadetId = selectedCadet
                    val typeId = selectedType
                    if (add && cadetId != null && typeId != null) {
                        viewModel.createEvent(selectedLocalDateTime, cadetId, typeId)
                    }
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
package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

@Preview
@Composable
fun Calendar(
    modifier: Modifier = Modifier,
    selectedMonth: Int = 1
) {
    val months = listOf("январь", "февраль",
        "март", "апрель", "май",
        "июнь", "июль", "август",
        "сентябрь", "октябрь", "ноябрь",
        "декабрь")
    var expanded by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Button(onClick = { expanded = !expanded }) {
            Text("Выберите месяц...")
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }) },
                    onClick = { /* Do something... */ }
                )
            }
        }
    }
}

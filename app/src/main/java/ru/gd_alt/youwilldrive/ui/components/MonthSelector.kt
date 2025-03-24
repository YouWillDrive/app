package ru.gd_alt.youwilldrive.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.gd_alt.youwilldrive.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Preview
@Composable
fun MonthSelector(
    modifier: Modifier = Modifier,
    defaultMonth: Int = LocalDate.now().monthValue,
    defaultYear: Int = LocalDate.now().year,
    maxOffset: Int = 2,
    onMonthYearChanged: (month: Int, year: Int) -> Unit = { month, year ->
        val selectedDate = "$month.$year"
        android.util.Log.d("MonthSelector", "Selected date: $selectedDate") }
) {
    val currentDate = remember { LocalDate.now() }
    var selectedDate by remember {
        mutableStateOf(YearMonth.of(defaultYear, defaultMonth))
    }

    // Calculate min and max allowed dates
    val minDate = remember {
        YearMonth.from(currentDate).minusMonths(maxOffset.toLong())
    }
    val maxDate = remember {
        YearMonth.from(currentDate).plusMonths(maxOffset.toLong())
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous month button
            IconButton(
                onClick = {
                    val newDate = selectedDate.minusMonths(1)
                    if (newDate.isAfter(minDate) || newDate.equals(minDate)) {
                        selectedDate = newDate
                        onMonthYearChanged(selectedDate.monthValue, selectedDate.year)
                    }
                },
                enabled = selectedDate.isAfter(minDate)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_month)
                )
            }

            // Month and year display
            Text(
                text = "${selectedDate.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))} ${selectedDate.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Next month button
            IconButton(
                onClick = {
                    val newDate = selectedDate.plusMonths(1)
                    if (newDate.isBefore(maxDate) || newDate.equals(maxDate)) {
                        selectedDate = newDate
                        onMonthYearChanged(selectedDate.monthValue, selectedDate.year)
                    }
                },
                enabled = selectedDate.isBefore(maxDate)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month)
                )
            }
        }
    }
}
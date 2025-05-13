package ru.gd_alt.youwilldrive.ui.screens.Chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun ChatScreen(
    chatHistory: List<String> = listOf(
        "Привет",
        "Привет",
        "Завтра в 3",
        "Да",
        "*15",
        ".",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed aliquam, arcu quis varius euismod, augue urna bibendum lacus, sed euismod nibh quam id dui. Donec eget ligula lobortis, tincidunt turpis a, molestie lacus. Integer id quam sit amet lorem elementum condimentum. Vivamus feugiat lacus ac auctor aliquam. Vestibulum dictum diam dolor, non egestas arcu varius sed. Integer in metus ac massa venenatis ultricies. Sed nec sapien a enim efficitur blandit. Duis mollis justo vel ligula sodales aliquam. Quisque posuere lobortis mollis. Etiam nec ante purus. Aliquam luctus fermentum venenatis. Pellentesque id magna ut nunc mattis ultricies sed eget nibh. Aenean in sem lobortis, auctor tellus vel, molestie libero. Suspendisse potenti.",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed aliquam, arcu quis varius euismod, augue urna bibendum lacus, sed euismod nibh quam id dui."
    )
) {
    Column {
        for (i in chatHistory.indices) {
            if (i % 2 == 0) {
                RightMessage(chatHistory[i])
            }
            else {
                LeftMessage(chatHistory[i])
            }
        }
    }
}

@Preview
@Composable
fun LeftMessage(text: String = "Lorem Ipsum") {
    Row(
        Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text,
                Modifier.padding(10.dp)
            )
        }
    }
}

@Preview
@Composable
fun RightMessage(text: String = "Lorem Ipsum") {
    Row(
        Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text,
                Modifier.padding(10.dp)
            )
        }
    }
}
package ru.gd_alt.youwilldrive.ui.screens.Chat

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.gd_alt.youwilldrive.data.DataStoreManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

enum class MessageType {
    SENT,
    RECEIVED,
    SYSTEM
}

data class ChatMessage(
    val text: String,
    val type: MessageType,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Formats a LocalDateTime object to a string representing only the time (HH:mm).
 */
fun LocalDateTime.toDateTimeString(): String {
    return this.format(DateTimeFormatter.ofPattern("HH:mm"))
}

/**
 * Formats a LocalDate object to a human-readable date string,
 * showing "Сегодня" for today, "Вчера" for yesterday, or "dd.MM.yyyy" otherwise.
 */
fun LocalDate.toDateString(): String {
    val today = LocalDate.now()
    val yesterday = LocalDate.now().minusDays(1)

    return when (this) {
        today -> "Сегодня"
        yesterday -> "Вчера"
        else -> {
            this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreen(
    recepientId: String? = null,
    initialMessages: List<ChatMessage> = listOf<ChatMessage>()
) {
    val context = LocalContext.current.applicationContext
    val dataStoreManager = remember { DataStoreManager(context) }
    val factory = remember(dataStoreManager) {
        ChatViewModelFactory(dataStoreManager, recepientId.toString())
    }

    val viewModel: ChatViewModel = viewModel(factory = factory)

    LaunchedEffect(Unit) {
        viewModel.loadChatHistory()
    }

    val viewModelScope = viewModel.viewModelScope
    val listState = rememberLazyListState()

    val chatHistory by viewModel.chatHistoryWithDates.collectAsState()
    val chatHistoryWithDates by viewModel.chatHistoryWithDates.collectAsState()

    var newMessage by viewModel.newMessage

    if (recepientId != null) {
        Log.i("ChatScreen", "Recipient ID: $recepientId")
    }

    // Scroll to the bottom when the list of displayed messages (including system messages) changes
    LaunchedEffect(chatHistoryWithDates.size) {
        if (chatHistoryWithDates.isNotEmpty()) {
            listState.animateScrollToItem(chatHistoryWithDates.size - 1)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 8.dp)) {

        // Display the recipient's name at the top
        Text(
            text = viewModel.recipientName.collectAsState().value.toString(),
            style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp) // Spacing between chat message items
        ) {
            itemsIndexed(chatHistory) { index, message ->
                ChatMessageItem(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { viewModel.updateNewMessage(it) },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                placeholder = { Text("…") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (newMessage.text.isNotBlank()) {
                        viewModelScope.launch {
                            viewModel.sendMessage()
                        }
                        newMessage = TextFieldValue("")
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = when (message.type) {
        MessageType.SENT -> Alignment.End
        MessageType.RECEIVED -> Alignment.Start
        MessageType.SYSTEM -> Alignment.CenterHorizontally
    }
    val cardColors = when (message.type) {
        MessageType.SENT -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
        MessageType.RECEIVED -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
        MessageType.SYSTEM -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
    val textColor = when (message.type) {
        MessageType.SENT -> MaterialTheme.colorScheme.onPrimary
        MessageType.RECEIVED -> MaterialTheme.colorScheme.onSecondaryContainer
        MessageType.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 300.dp)
                .padding(vertical = 4.dp), // Padding around the card content
            colors = cardColors,
            shape = RoundedCornerShape(
                topStart = if (message.type == MessageType.RECEIVED || message.type == MessageType.SYSTEM) 4.dp else 16.dp,
                topEnd = if (message.type == MessageType.SENT || message.type == MessageType.SYSTEM) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = if (message.type == MessageType.SYSTEM) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
                )
                if (message.type != MessageType.SYSTEM) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestamp.toDateTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
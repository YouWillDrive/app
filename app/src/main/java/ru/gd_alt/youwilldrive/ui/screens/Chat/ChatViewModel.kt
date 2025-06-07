package ru.gd_alt.youwilldrive.ui.screens.Chat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.gd_alt.youwilldrive.data.DataStoreManager
import java.time.LocalDate

class ChatViewModel(
    dataStoreManager: DataStoreManager,
    recepientId: String
) : ViewModel() {
    private val _rawMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    var newMessage = mutableStateOf(TextFieldValue(""))
        private set

    // StateFlow for the chat history including date separators (for UI consumption)
    val chatHistoryWithDates: StateFlow<List<ChatMessage>> = _rawMessages
        .map { messages ->
            processMessagesWithDates(messages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun processMessagesWithDates(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) return emptyList()

        val processedMessages = mutableListOf<ChatMessage>()
        var lastDate: LocalDate? = null

        messages.forEach { message ->
            val messageDate = message.timestamp.toLocalDate()
            if (messageDate != lastDate) {
                processedMessages.add(
                    ChatMessage(
                        text = messageDate.toDateString(),
                        type = MessageType.SYSTEM,
                        timestamp = message.timestamp
                    )
                )
                lastDate = messageDate
            }
            processedMessages.add(message)
        }
        return processedMessages
    }

    suspend fun sendMessage() {
        val text = newMessage.value.text
        if (text.isNotBlank()) {
            val newMsg = ChatMessage(text = text, type = MessageType.SENT)
            // Add to the raw messages list
            _rawMessages.value = _rawMessages.value + newMsg
            // Clear the input field
            newMessage.value = TextFieldValue("")
        }
    }

    fun updateNewMessage(newValue: TextFieldValue) {
        newMessage.value = newValue
    }

    fun receiveMessage(message: ChatMessage) {
        _rawMessages.value = _rawMessages.value + message
    }
}
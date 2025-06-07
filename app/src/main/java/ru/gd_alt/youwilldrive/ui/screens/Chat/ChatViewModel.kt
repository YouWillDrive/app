package ru.gd_alt.youwilldrive.ui.screens.Chat

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.toJavaLocalDateTime
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Chat
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.models.fetchRelatedSingle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.data.client.Connection
import ru.gd_alt.youwilldrive.data.client.LiveQueryUpdate
import ru.gd_alt.youwilldrive.data.client.LiveUpdateCallback
import ru.gd_alt.youwilldrive.data.models.RecordID
import ru.gd_alt.youwilldrive.models.Message
import java.time.LocalDate

class ChatViewModel(
    private val dataStoreManager: DataStoreManager,
    private val recepientId: String
) : ViewModel() {
    private val _rawMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    var newMessage = mutableStateOf(TextFieldValue(""))
        private set

    private var liveQueryId: String? = null

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

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    private val _recipientName = MutableStateFlow<String?>(null)
    val recipientName: StateFlow<String?> = _recipientName.asStateFlow()

    fun loadChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the User objects for me and the recipient
                val myUserId = dataStoreManager.getUserId().first()
                Log.d("ChatViewModel", "Loading chat for recipient ID: $recepientId")
                Log.d("ChatViewModel", "My user ID: $myUserId")
                val me = User.fromId(myUserId.toString()) ?: return@launch
                val recipient = User.fromId(recepientId) ?: return@launch
                _recipientName.value = "${recipient.name} ${recipient.patronymic} ${recipient.surname}"

                // Find an existing chat or create a new one
                var chatSession = Chat.byParticipants(me, recipient)
                _chat.value = chatSession

                // Load messages from the chat
                val messagesFromDb = Message.allWithChatAndSender(
                    chatSession?.id ?: return@launch)

                Log.d("ChatViewModel", "Loaded messages: $messagesFromDb")

                // Convert database Message models to UI ChatMessage models
                val uiMessages = messagesFromDb.map { dbMessage ->
                    // Determine if the message was sent or received
                    val senderId = dbMessage.expansions["sender"]!!.toString()
                    val messageType = if (senderId == myUserId) {
                        MessageType.SENT
                    } else {
                        MessageType.RECEIVED
                    }
                    ChatMessage(
                        text = dbMessage.text,
                        type = messageType,
                        timestamp = dbMessage.dateSent.toJavaLocalDateTime()
                    )
                }
                _rawMessages.value = uiMessages.sortedBy { it.timestamp } as List<ChatMessage>

                // Kill any previous live query before starting a new one
                killLiveQuery()

                // Start live query for new messages
                val query = "LIVE SELECT * FROM belongs_to WHERE out = ${_chat.value!!.id}"
                liveQueryId = Connection.cl.liveQuery(query, null, LiveUpdateCallback.Async { update ->
                    if (update is LiveQueryUpdate.Create) {
                        // A new 'belongs_to' relationship was created, meaning a new message arrived
                        val messageId = (update.data["in"] as? RecordID)?.toString() ?: return@Async

                        // Fetch the full message
                        val newDbMessage = Message.fromId(messageId)
                        if (newDbMessage != null) {
                            // Make sure not to add our own sent message again
                            val sender = newDbMessage.fetchRelatedSingle("sent_by", User::fromId)
                            Log.d("ChatViewModel", "New message received: $newDbMessage from sender: $sender")
                            val senderId = sender?.id ?: return@Async
                            val uiMessage = ChatMessage(
                                text = newDbMessage.text,
                                type = if (senderId == myUserId) MessageType.SENT else MessageType.RECEIVED,
                                timestamp = newDbMessage.dateSent.toJavaLocalDateTime()
                            )
                            _rawMessages.value = _rawMessages.value + uiMessage
                        }
                    }
                })

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load chat history", e)
            }
        }
    }

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

    fun sendMessage() {
        val text = newMessage.value.text
        Log.i("ChatViewModel", "Attempting to send message: $text (not blank: ${text.isNotBlank()}) into ${_chat.value}")
        if (text.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                if (text.isNotBlank() && _chat.value != null) {
                    try {
                        val myUserId = dataStoreManager.getUserId().first()
                        Log.d("ChatViewModel", "My user ID: $myUserId")

                        val me = User.fromId(myUserId.toString()) ?: return@launch

                        // 1. Create the message in the database
                        val newDbMessage = Message.create(text)
                        Log.d("ChatViewModel", "Created new message: $newDbMessage")

                        // 2. Relate it to the current chat
                        Chat.sendMessage(_chat.value!!, newDbMessage, me)

                        withContext(Dispatchers.Main) {
                            newMessage.value = TextFieldValue("") // Clear input
                        }

                    } catch(e: Exception) {
                        Log.e("ChatViewModel", "Failed to send message", e)
                    }
                }
            }
        }
    }

    fun updateNewMessage(newValue: TextFieldValue) {
        newMessage.value = newValue
    }

    private fun killLiveQuery() {
        liveQueryId?.let {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Connection.cl.kill(it)
                    Log.d("ChatViewModel", "Killed live query: $it")
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Failed to kill live query $it", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        killLiveQuery()
    }
}
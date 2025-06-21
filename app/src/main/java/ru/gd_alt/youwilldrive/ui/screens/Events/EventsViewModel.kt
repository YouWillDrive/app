package ru.gd_alt.youwilldrive.ui.screens.Events

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.User


sealed class EventsState {
    data object Idle : EventsState()
    data object Loading : EventsState()
}

class EventsViewModel(
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Idle)
    val eventsState = _eventsState.asStateFlow()

    private val _events = MutableStateFlow<List<Event>?>(null)
    val events: StateFlow<List<Event>?> = _events.asStateFlow()

    private val userId: StateFlow<String?> = dataStoreManager.getUserId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchEvents() {
        _eventsState.value = EventsState.Loading
        var events: List<Event> = emptyList()
        var error: String? = null

        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }
            try {
                val user: User? = User.fromId(actualUserId.toString())
                events = ((user?.isCadet() ?: user?.isInstructor()
                    )?.events() ?: emptyList())

                Log.d("fetchEvent", "Loaded events: $events")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                _events.value = events.sortedBy { it.date }
                _eventsState.value = EventsState.Idle
            }
        }
    }

    fun acceptEvent(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }.toString()
            try {
                val event = Event.fromId(eventId)!!

                event.confirmToHappen(actualUserId, true)
                Notification.postNotification(
                    "Событие на ${event.date.date} подтверждено!",
                    "Подтверждено событие в ${event.date}",
                    emptyList(),
                    getOppositeUser(actualUserId, event)
                )
            } catch (e: Exception) {
                Log.e("acceptEvent", "Error while confirming event", e)
            }
        }
    }


    fun declineEvent(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }.toString()
            try {
                val event = Event.fromId(eventId)!!

                event.confirmToHappen(actualUserId, false)
                Notification.postNotification(
                    "Событие на ${event.date.date} отменено!",
                    "Отменено событие в ${event.date}",
                    emptyList(),
                    getOppositeUser(actualUserId, event)
                )
            } catch (e: Exception) {
                Log.e("acceptEvent", "Error while declining event", e)
            }
        }
    }

    fun confirmDuration(eventId: String, hours: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }.toString()
            try {
                Event.fromId(eventId)!!.confirmDuration(actualUserId, hours)
            } catch (e: Exception) {
                Log.e("acceptEvent", "Error while confirming event", e)
            }
        }
    }

    fun acceptDuration(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }.toString()
            try {
                val event = Event.fromId(eventId)!!

                event.acceptDuration(actualUserId, true)
                Notification.postNotification(
                    "Длительность события подтверждена!",
                    "Событие длилось ${event.actualConfirmationValue("confirmation_types:duration")} ч.",
                    emptyList(),
                    getOppositeUser(actualUserId, event)
                )
            } catch (e: Exception) {
                Log.e("acceptEvent", "Error while accepting duration", e)
            }
        }
    }

    fun declineDuration(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }.toString()
            try {
                val event = Event.fromId(eventId)!!
                event.acceptDuration(actualUserId, false)
                Notification.postNotification(
                    "Длительность события отклонена!",
                    "Необходимо подтвердить снова, событие длилось не ${event.actualConfirmationValue("confirmation_types:duration")} ч.",
                    emptyList(),
                    getOppositeUser(actualUserId, event)
                )
            } catch (e: Exception) {
                Log.e("acceptEvent", "Error while declining duration", e)
            }
        }
    }

    fun postpone(event: Event, millis: Long) {
        viewModelScope.launch {
            val actualUserId = userId.first { !it.isNullOrEmpty() }.toString()

            event.postpone(actualUserId, millis)
            Notification.postNotification(
                "Перенос занятия",
                "Событие на ${{ Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date}} перенесено. Требуется подтверждение.",
                emptyList(),
                getOppositeUser(actualUserId, event)
            )
        }
    }
}

private suspend fun getOppositeUser(userId: String, event: Event): String {
    var receiver = ""
    val instructorUserId = event.ofInstructor()?.me()?.id
    val cadetUserId = event.ofCadet()?.me()?.id

    if (cadetUserId == userId) {
        receiver = instructorUserId ?: ""
    }
    if (instructorUserId == userId) {
        receiver = cadetUserId ?: ""
    }

    if (receiver.isEmpty()) {
        Notification.postNotification(
            "Не удалось отправить уведомление",
            "Вам необходимо самостоятельно связаться инструктором/курсантом, касательно вашего последнего действия.",
            emptyList(),
            userId
        )
    }

    return receiver
}
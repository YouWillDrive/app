package ru.gd_alt.youwilldrive.ui.screens.EventEdit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.EventType
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.User

sealed class EventEditState {
    data object Idle : EventEditState()
    data object Loading : EventEditState()
}


class EventEditViewModel(
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _eventEditState = MutableStateFlow<EventEditState>(EventEditState.Idle)
    val calendarState = _eventEditState.asStateFlow()

    private val _cadets = MutableStateFlow<List<Pair<String, String>>>(mutableListOf())
    val cadets: StateFlow<List<Pair<String, String>>> = _cadets.asStateFlow()

    private val userId: StateFlow<String?> = dataStoreManager.getUserId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchCadetsIdName(onResponse: (List<Pair<String, String>>, String?) -> Unit) {
        _eventEditState.value = EventEditState.Loading
        val cadets: MutableList<Pair<String, String>> = mutableListOf()
        var error: String? = null

        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { it -> !it.isNullOrEmpty() }
            try {
                val user: User? = User.fromId(actualUserId.toString())
                for (cadet in user?.isInstructor()?.cadets() ?: emptyList()) {
                    cadets.add(
                        cadet.id to (cadet.me()?.fullNameShort ?: cadet.id)
                    )
                }
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(cadets, error)
                _eventEditState.value = EventEditState.Idle
            }
        }
    }

    fun fetchEventTypes(onResponse: (List<Pair<String, String>>, String?) -> Unit) {
        val types: MutableList<Pair<String, String>> = mutableListOf()
        var error: String? = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (type in EventType.all()) {
                    types.add(
                        type.id to type.name
                    )
                }
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(types, error)
                _eventEditState.value = EventEditState.Idle
            }
        }
    }

    fun createEvent(dateTime: LocalDateTime, cadetId: String, typeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { !it.isNullOrEmpty() }
            val instructor = User.fromId(actualUserId ?: "")?.isInstructor() ?: return@launch
            try {
                val eventId = Event.create(dateTime)
                val newEvent = Event.fromId(eventId) ?: return@launch
                newEvent.addParticipants(instructor.id, cadetId)
                newEvent.setType(typeId)
                Log.d("createEvent", "${dateTime.date}")
                Notification.postNotification(
                    "Новое событие",
                    "Подтвердите событие на ${dateTime.date}",
                    emptyList(),
                    Cadet.fromId(cadetId)!!.me()!!.id
                )
            } catch (e: Exception) {
                Log.e("createEvent", "Error while creating event.", e)
            }
        }
    }
}
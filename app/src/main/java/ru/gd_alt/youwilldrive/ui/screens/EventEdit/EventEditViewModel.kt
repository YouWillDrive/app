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
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.ui.screens.Calendar.CalendarState

sealed class EventEditState {
    data object Idle : EventEditState()
    data object Loading : EventEditState()
}


class EventEditViewModel(
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _eventEditState = MutableStateFlow<EventEditState>(EventEditState.Idle)
    val calendarState = _eventEditState.asStateFlow()

    private val _cadets = MutableStateFlow<List<Cadet>?>(null)
    val cadets: StateFlow<List<Cadet>?> = _cadets.asStateFlow()

    private val userId: StateFlow<String?> = dataStoreManager.getUserId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchCadets() {
        _eventEditState.value = EventEditState.Loading
        var cadets: List<Cadet>? = null
        var error: String? = null

        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { it -> !it.isNullOrEmpty() }
            try {
                val user: User? = User.fromId(actualUserId.toString())
                cadets = /* user?.isInstructor()?.cadets() ?: */ emptyList() // TODO
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                _cadets.value = cadets
                _eventEditState.value = EventEditState.Idle
            }
        }
    }
}
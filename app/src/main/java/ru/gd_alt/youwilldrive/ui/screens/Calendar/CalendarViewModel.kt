package ru.gd_alt.youwilldrive.ui.screens.Calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.User

sealed class CalendarState {
    data object Idle : CalendarState()
    data object Loading : CalendarState()
}

class CalendarViewModel : ViewModel() {
    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Loading)
    val calendarState = _calendarState.asStateFlow()

    fun fetchEvents(userId: String, onResponse: (List<Event>?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _calendarState.value = CalendarState.Loading
            var events: List<Event>? = null
            var error: String? = null
            try {
                val user: User? = User.fromId(userId)
                events = (user?.isCadet() ?: user?.isInstructor())?.events() ?: listOf()
                Log.d("fetchEvent", "Loaded events: $events")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(events, error)
                _calendarState.value = CalendarState.Idle
            }
        }

    }
}
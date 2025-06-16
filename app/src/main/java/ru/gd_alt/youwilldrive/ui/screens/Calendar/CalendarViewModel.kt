package ru.gd_alt.youwilldrive.ui.screens.Calendar

import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Event
import ru.gd_alt.youwilldrive.models.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first

sealed class CalendarState {
    data object Idle : CalendarState()
    data object Loading : CalendarState()
    data class Error(val message: String) : CalendarState()
}

class CalendarViewModel(
    dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Idle)
    val calendarState = _calendarState.asStateFlow()

    private val _events = MutableStateFlow<List<Event>?>(null)
    val events: StateFlow<List<Event>?> = _events.asStateFlow()

    private val userId: StateFlow<String?> = dataStoreManager.getUserId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchEvents() {
        _calendarState.value = CalendarState.Loading
        var events: List<Event>? = null
        var error: String? = null

        viewModelScope.launch(Dispatchers.IO) {
            val actualUserId = userId.first { it -> !it.isNullOrEmpty() }
            try {
                val user: User? = User.fromId(actualUserId.toString())
                events = (
                    (
                        user?.isCadet() ?: user?.isInstructor()
                    )?.events() ?: emptyList()
                ).fastFilter {
                    it.actualConfirmationValue("confirmation_types:to_happen") == 1L
                }
                Log.d("fetchEvent", "Loaded events: $events")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                _events.value = events
                _calendarState.value = CalendarState.Idle
            }
        }
    }
}
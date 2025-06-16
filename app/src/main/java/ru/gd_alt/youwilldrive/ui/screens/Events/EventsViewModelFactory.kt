package ru.gd_alt.youwilldrive.ui.screens.Events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gd_alt.youwilldrive.data.DataStoreManager

class EventsViewModelFactory(
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventsViewModel::class.java)) {
            // Create and return CalendarViewModel, passing the dependency
            return EventsViewModel(dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
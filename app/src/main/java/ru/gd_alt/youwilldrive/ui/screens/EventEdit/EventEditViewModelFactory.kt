package ru.gd_alt.youwilldrive.ui.screens.EventEdit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gd_alt.youwilldrive.data.DataStoreManager

class EventEditViewModelFactory(
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventEditViewModel::class.java)) {
            // Create and return EventEditViewModel, passing the dependency
            return EventEditViewModel(dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
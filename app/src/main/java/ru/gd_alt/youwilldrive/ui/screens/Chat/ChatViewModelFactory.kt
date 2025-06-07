package ru.gd_alt.youwilldrive.ui.screens.Chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gd_alt.youwilldrive.data.DataStoreManager

// Factory to create CalendarViewModel with DataStoreManager dependency
class ChatViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val recepientId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            // Create and return ChatViewModel, passing the dependency
            return ChatViewModel(dataStoreManager, recepientId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
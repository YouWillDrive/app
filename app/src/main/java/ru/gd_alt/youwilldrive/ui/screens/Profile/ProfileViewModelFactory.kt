package ru.gd_alt.youwilldrive.ui.screens.Calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.ui.screens.Profile.ProfileViewModel

// Factory to create CalendarViewModel with DataStoreManager dependency
class ProfileViewModelFactory(
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
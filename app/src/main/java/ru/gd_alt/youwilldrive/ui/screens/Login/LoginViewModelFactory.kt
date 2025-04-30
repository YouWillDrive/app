package ru.gd_alt.youwilldrive.ui.screens.Calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.ui.screens.Login.LoginViewModel
import ru.gd_alt.youwilldrive.ui.screens.Profile.ProfileViewModel

// Factory to create CalendarViewModel with DataStoreManager dependency
class LoginViewModelFactory(
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            // Create and return CalendarViewModel, passing the dependency
            return LoginViewModel(dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
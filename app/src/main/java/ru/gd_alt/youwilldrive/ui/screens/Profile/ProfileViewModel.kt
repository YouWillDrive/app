package ru.gd_alt.youwilldrive.ui.screens.Profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.User

sealed class ProfileState {
    data object Idle : ProfileState()
    data object Loading : ProfileState()
}

class ProfileViewModel: ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val calendarState = _profileState.asStateFlow()

    fun fetchData(userId: String, onResponse: (Any?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = User.fromId(userId)
            var data: Any? = null
            var error: String? = null
            _profileState.value = ProfileState.Loading
            try {
                data = user?.isCadet() ?: user?.isInstructor()
            }
            catch (e: Exception) {
                error = e.message
            }

            withContext(Dispatchers.Main) {
                onResponse(data, error)
                _profileState.value = ProfileState.Idle
            }
        }
    }
}
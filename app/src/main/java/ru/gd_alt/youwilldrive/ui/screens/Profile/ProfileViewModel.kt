package ru.gd_alt.youwilldrive.ui.screens.Profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.Participant
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.data.DataStoreManager

sealed class ProfileState {
    data object Idle : ProfileState()
    data object Loading : ProfileState()
}

class ProfileViewModel(
    private val dataStoreManager: DataStoreManager
): ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState = _profileState.asStateFlow()

    private val _userDataState = MutableStateFlow<Participant?>(null)
    val userDataState: StateFlow<Participant?> = _userDataState.asStateFlow()

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    private val userId: StateFlow<String?> = dataStoreManager.getUserId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchData() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = User.fromId(userId.toString())
            var data: Participant? = null
            var error: String? = null
            _profileState.value = ProfileState.Loading
            try {
                data = user?.isCadet() ?: user?.isInstructor()
                Log.d("fetchData", "$data")
            }
            catch (e: Exception) {
                error = e.message
            }

            withContext(Dispatchers.Main) {
                _userDataState.value = data
                _userState.value = user
                _profileState.value = ProfileState.Idle
            }
        }
    }
}
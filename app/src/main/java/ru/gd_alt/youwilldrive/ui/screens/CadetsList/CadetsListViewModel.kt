package ru.gd_alt.youwilldrive.ui.screens.CadetsList

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.User

sealed class CadetsListState {
    data object Idle : CadetsListState()
    data object Loading : CadetsListState()
}

class CadetListsViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {
    private val _cadetsListState = MutableStateFlow<CadetsListState>(CadetsListState.Loading)
    val cadetsListState = _cadetsListState.asStateFlow()

    private val _cadets = MutableStateFlow<List<Cadet>>(emptyList())
    val cadets: StateFlow<List<Cadet>> = _cadets.asStateFlow()

    fun loadCadets() {
        viewModelScope.launch(Dispatchers.IO) {
            _cadetsListState.value = CadetsListState.Loading
            val userId = dataStoreManager.getUserId().first { it != null && it.isNotEmpty() }

            try {
                val user = User.fromId(userId.toString())
                val instructor = user?.isInstructor()
                if (instructor != null) {
                    val cadetsList = instructor.cadets()
                    _cadets.value = cadetsList
                } else {
                    Log.w("CadetListsViewModel", "Logged in user is not an instructor.")
                    _cadets.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("CadetListsViewModel", "Failed to fetch cadets", e)
                _cadets.value = emptyList()
            } finally {
                withContext(Dispatchers.Main) {
                    _cadetsListState.value = CadetsListState.Idle
                }
            }
        }
    }

    fun fetchCadetUser(cadet: Cadet, onResponse: (User?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var cadetUser: User? = null
            var error: String? = null

            try {
                cadetUser = cadet.me()
                Log.d("fetchCadetUser", "Loaded cadetUser: $cadetUser")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(cadetUser, error)
            }
        }
    }
}
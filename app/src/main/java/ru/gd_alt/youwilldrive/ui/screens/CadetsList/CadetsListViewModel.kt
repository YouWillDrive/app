package ru.gd_alt.youwilldrive.ui.screens.CadetsList

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Instructor
import ru.gd_alt.youwilldrive.models.Plan
import ru.gd_alt.youwilldrive.models.User

sealed class CadetsListState {
    data object Idle : CadetsListState()
    data object Loading : CadetsListState()
}

class CadetListsViewModel : ViewModel() {
    private val _cadetsListState = MutableStateFlow<CadetsListState>(CadetsListState.Loading)
    val cadetsListState = _cadetsListState.asStateFlow()

    fun fetchCadets(instructor: Instructor, onResponse: (List<Cadet>?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _cadetsListState.value = CadetsListState.Loading
            var cadetsList: List<Cadet>? = null
            var error: String? = null
            try {
                cadetsList = instructor.cadets()
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(cadetsList, error)
                _cadetsListState.value = CadetsListState.Idle
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
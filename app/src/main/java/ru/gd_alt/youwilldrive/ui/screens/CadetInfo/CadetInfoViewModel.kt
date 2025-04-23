package ru.gd_alt.youwilldrive.ui.screens.CadetInfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Plan

sealed class CadetInfoState {
    data object Idle : CadetInfoState()
    data object Loading : CadetInfoState()
}

class CadetInfoViewModel : ViewModel() {
    private val _cadetInfoState = MutableStateFlow<CadetInfoState>(CadetInfoState.Loading)
    val cadetInfoState = _cadetInfoState.asStateFlow()

    fun fetchPlan(cadet: Cadet, onResponse: (Plan?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _cadetInfoState.value = CadetInfoState.Loading
            var plan: Plan? = null
            var error: String? = null
            try {
                plan = cadet.actualPlanPoint()?.relatedPlan()
                Log.d("fetchData", "Loaded plan: $plan")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(plan, error)
                _cadetInfoState.value = CadetInfoState.Idle
            }
        }

    }
}
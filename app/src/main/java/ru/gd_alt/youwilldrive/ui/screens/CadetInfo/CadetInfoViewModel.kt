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
import ru.gd_alt.youwilldrive.models.Instructor
import ru.gd_alt.youwilldrive.models.Plan
import ru.gd_alt.youwilldrive.models.User

sealed class PlanState {
    data object Idle : PlanState()
    data object Loading : PlanState()
}
sealed class InstructorState {
    data object Idle : InstructorState()
    data object Loading : InstructorState()
}

class CadetInfoViewModel : ViewModel() {
    private val _planState = MutableStateFlow<PlanState>(PlanState.Loading)
    val planState = _planState.asStateFlow()
    private val _instructorState = MutableStateFlow<InstructorState>(InstructorState.Loading)
    val instructorState = _instructorState.asStateFlow()

    fun fetchPlan(cadet: Cadet, onResponse: (Plan?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _planState.value = PlanState.Loading
            var plan: Plan? = null
            var error: String? = null
            try {
                plan = cadet.actualPlanPoint()?.relatedPlan()
                Log.d("fetchPlan", "Loaded plan: $plan")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(plan, error)
                _planState.value = PlanState.Idle
            }
        }
    }

    fun fetchInstructorUser(cadet: Cadet, onResponse: (User?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _instructorState.value = InstructorState.Loading
            var instructor: User? = null
            var error: String? = null

            try {
                instructor = cadet.actualPlanPoint()?.assignedInstructor()?.me()
                Log.d("fetchInstructor", "Loaded instructor: $instructor")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(instructor, error)
                _planState.value = PlanState.Idle
            }
        }
    }
}
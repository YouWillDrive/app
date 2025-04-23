package ru.gd_alt.youwilldrive.ui.screens.InstructorInfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.Car
import ru.gd_alt.youwilldrive.models.Instructor


sealed class InstructorInfoState {
    data object Idle : InstructorInfoState()
    data object Loading : InstructorInfoState()
}

class InstructorInfoViewModel : ViewModel() {
    private val _instructorInfoState = MutableStateFlow<InstructorInfoState>(InstructorInfoState.Loading)
    val instructorInfoState = _instructorInfoState.asStateFlow()

    fun fetchCars(instructor: Instructor, onResponse: (List<Car>?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _instructorInfoState.value = InstructorInfoState.Loading
            var cars: List<Car>? = null
            var error: String? = null
            try {
                cars = instructor.cars()
                Log.d("fetchCars", "Loaded plan: $cars")
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(cars, error)
                _instructorInfoState.value = InstructorInfoState.Idle
            }
        }

    }
}
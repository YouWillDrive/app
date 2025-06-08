package ru.gd_alt.youwilldrive.ui.screens.CadetInfo

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Instructor
import ru.gd_alt.youwilldrive.models.Plan
import ru.gd_alt.youwilldrive.models.User
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

    private val _instructorAvatarBitmap = MutableStateFlow<ImageBitmap?>(null)
    val instructorAvatarBitmap: StateFlow<ImageBitmap?> = _instructorAvatarBitmap.asStateFlow()

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

    @OptIn(ExperimentalEncodingApi::class)
    fun fetchInstructorUser(cadet: Cadet, onResponse: (User?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _instructorState.value = InstructorState.Loading
            var instructorUser: User? = null
            var error: String? = null

            try {
                instructorUser = cadet.actualPlanPoint()?.assignedInstructor()?.me()
                Log.d("fetchInstructor", "Loaded instructor: $instructorUser")
                instructorUser?.avatarPhoto?.let { base64String ->
                    if (base64String.isNotBlank()) {
                        withContext(Dispatchers.Default) { // Decode on a background dispatcher
                            try {
                                val cleanedBase64String = base64String.replace("\\s".toRegex(), "")
                                val decodedBytes = Base64.decode(cleanedBase64String)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                _instructorAvatarBitmap.value = bitmap?.asImageBitmap()
                            } catch (e: Exception) {
                                Log.e("CadetInfoViewModel", "Error decoding instructor avatar Base64: ${e.message}")
                                _instructorAvatarBitmap.value = null
                            }
                        }
                    } else {
                        _instructorAvatarBitmap.value = null // Clear avatar if string is empty
                    }
                } ?: run {
                    _instructorAvatarBitmap.value = null // Clear avatar if no photo string
                }
            } catch (e: Exception) {
                error = e.message
            }
            withContext(Dispatchers.Main) {
                onResponse(instructorUser, error)
                _instructorState.value = InstructorState.Idle
            }
        }
    }
}
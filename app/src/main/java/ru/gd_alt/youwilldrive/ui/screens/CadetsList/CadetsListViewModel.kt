package ru.gd_alt.youwilldrive.ui.screens.CadetsList

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.add
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class CadetsListState {
    data object Idle : CadetsListState()
    data object Loading : CadetsListState()
}

class CadetListsViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {
    private val _cadetsListState = MutableStateFlow<CadetsListState>(CadetsListState.Loading)
    val cadetsListState = _cadetsListState.asStateFlow()

    private val _cadets = MutableStateFlow<List<Cadet>>(emptyList())
    val cadets: StateFlow<List<Cadet>> = _cadets.asStateFlow()

    private val _cadetAvatars = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())
    val cadetAvatars: StateFlow<Map<String, ImageBitmap?>> = _cadetAvatars.asStateFlow()

    fun loadCadets() {
        viewModelScope.launch(Dispatchers.IO) {
            _cadetsListState.value = CadetsListState.Loading
            val userId = dataStoreManager.getUserId().first { it != null && it.isNotEmpty() }

            try {
                val user = User.fromId(userId.toString())
                val instructor = user?.isInstructor()
                if (instructor != null) {
                    val cadetsList = instructor.cadets()
                    val avatarFetchJobs = mutableListOf<Job>()
                    cadetsList.forEach { cadet ->
                        val job = viewModelScope.launch(Dispatchers.IO) {
                            fetchCadetUserAvatar(cadet)
                            Log.d("CadetListsViewModel", "Avatar fetched for cadet: ${cadet.id}")
                        }
                        avatarFetchJobs.add(job)
                    }
                    avatarFetchJobs.joinAll()
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

    @OptIn(ExperimentalEncodingApi::class)
    fun fetchCadetUserAvatar(cadet: Cadet) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = cadet.me()
                if (user != null) {
                    user.avatarPhoto.let { base64String ->
                        if (base64String.isNotBlank()) {
                            val cleanedBase64String = base64String.replace("\\s".toRegex(), "")
                            val decodedBytes = Base64.decode(cleanedBase64String)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            _cadetAvatars.value = _cadetAvatars.value + (user.id to bitmap?.asImageBitmap())
                        } else {
                            _cadetAvatars.value = _cadetAvatars.value + (user.id to null)
                        }
                    }
                } else {
                    Log.w("CadetListsViewModel", "No User found for cadet: ${cadet.id}")
                    _cadetAvatars.value = _cadetAvatars.value + (cadet.id to null)
                }
            } catch (e: Exception) {
                Log.e("CadetListsViewModel", "Error fetching/decoding avatar for cadet ${cadet.id}: ${e.message}")
                _cadetAvatars.value = _cadetAvatars.value + (cadet.id to null)
            }
        }
    }
}
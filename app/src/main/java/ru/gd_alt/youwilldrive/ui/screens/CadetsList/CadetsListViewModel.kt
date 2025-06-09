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
import ru.gd_alt.youwilldrive.models.Chat
import ru.gd_alt.youwilldrive.models.Message
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

    private val _unreadMessageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadMessageCounts: StateFlow<Map<String, Int>> = _unreadMessageCounts.asStateFlow()

    @OptIn(ExperimentalEncodingApi::class)
    fun loadCadets() {
        viewModelScope.launch(Dispatchers.IO) {
            _cadetsListState.value = CadetsListState.Loading
            val userId = dataStoreManager.getUserId().first { it != null && it.isNotEmpty() }

            try {
                val user = User.fromId(userId.toString())
                val instructor = user?.isInstructor()
                if (instructor != null) {
                    var cadetsList = instructor.cadets()
                    val allCadetsList = Cadet.allWithPhotos()

                    cadetsList = cadetsList.map { cadet ->
                        val fullCadet = allCadetsList.firstOrNull { it.id == cadet.id }
                        if (fullCadet != null && fullCadet.expansions.containsKey("avatar")) {
                            val avatarBase64 = fullCadet.expansions["avatar"].toString()
                            val cleanedBase64String = avatarBase64.replace("\\s".toRegex(), "")
                            val decodedBytes = Base64.decode(cleanedBase64String)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            val bitmapAvatar = bitmap?.asImageBitmap()
                            _cadetAvatars.value = (_cadetAvatars.value + (cadet.id to bitmapAvatar))
                            _unreadMessageCounts.value = _unreadMessageCounts.value + (
                                    cadet.id to Message.countUnreadInChat(
                                        Chat.byParticipants(cadet.me()!!, user)!!.id,
                                        user.id
                                    )
                                )
                        }
                        else {
                            _cadetAvatars.value = (_cadetAvatars.value + (cadet.id to null))
                        }
                        fullCadet ?: cadet
                    }.toMutableList()

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
package ru.gd_alt.youwilldrive.ui.screens.Profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.UtilsProvider
import ru.gd_alt.youwilldrive.models.Participant
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.data.client.Connection
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class ProfileState {
    data object Idle : ProfileState()
    data object Loading : ProfileState()
    data object UploadingImage : ProfileState()
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

    private val _userAvatarBitmap = MutableStateFlow<ImageBitmap?>(null)
    val userAvatarBitmap: StateFlow<ImageBitmap?> = _userAvatarBitmap.asStateFlow()

    private val userId: StateFlow<String?> = dataStoreManager.getUserId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalEncodingApi::class)
    fun fetchData() {
        viewModelScope.launch {
            var data: Participant? = null
            var error: String? = null
            _profileState.value = ProfileState.Loading
            val actualUserId = userId.first { !it.isNullOrEmpty() }
            val user = User.fromId(actualUserId.toString())
            Log.d("fetchData", "User ID: $actualUserId")
            Log.d("fetchData", "User: $user")
            try {
                data = user?.isCadet() ?: user?.isInstructor()
                user?.avatarPhoto?.let { base64String ->
                    if (base64String.isNotBlank()) {
                        withContext(Dispatchers.Default) { // Decode on a background dispatcher
                            try {
                                val cleanedBase64String = base64String.replace("\\s".toRegex(), "")
                                val decodedBytes = Base64.decode(cleanedBase64String)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                _userAvatarBitmap.value = bitmap?.asImageBitmap()
                            } catch (e: Exception) {
                                Log.e("ProfileViewModel", "Error decoding avatar Base64: ${e.message}")
                                _userAvatarBitmap.value = null
                            }
                        }
                    } else {
                        _userAvatarBitmap.value = null // Clear avatar if string is empty
                    }
                } ?: run {
                    _userAvatarBitmap.value = null // Clear avatar if no photo string
                }
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

    /**
     * Uploads a new profile photo for the current user.
     * The image will be resized, compressed, and Base64 encoded before upload.
     *
     * @param context The application context, needed for content resolver.
     * @param imageUri The URI of the image selected by the user.
     */
    fun uploadProfilePhoto(context: Context, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _profileState.value = ProfileState.UploadingImage // Set uploading state
            try {
                val base64Image = UtilsProvider.resizeAndEncodeImage(context, imageUri)
                val currentUserId = dataStoreManager.getUserId().first()
                if (base64Image != null && !currentUserId.isNullOrEmpty()) {
                    Log.d("ProfileViewModel", "Attempting to upload image for user: $currentUserId")
                    // Update the user's avatarPhoto field in SurrealDB
                    val updateResult = Connection.cl.query("UPDATE $currentUserId SET avatar = \$avatar", mapOf("avatar" to base64Image))
                    Log.d("ProfileViewModel", "Image upload result: $updateResult")
                    Log.d("ProfileViewModel", "Image uploaded successfully. Refreshing data...")
                    fetchData() // Refresh profile data including the new avatar
                } else {
                    Log.w("ProfileViewModel", "Failed to get base64 image or user ID is null/empty. Upload skipped.")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error uploading profile photo: ${e.message}", e)
            } finally {
                _profileState.value = ProfileState.Idle // Reset to idle after upload attempt
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreManager.clearUserId()
        }
    }
}
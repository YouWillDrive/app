package ru.gd_alt.youwilldrive.ui.screens.Notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.User

sealed class NotificationsState {
    data object Idle : NotificationsState()
    data object Loading : NotificationsState()
}

class NotificationsViewModel(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Idle)
    val notificationsState = _notificationsState.asStateFlow()

    private val _readNotificationsState = MutableStateFlow<List<Notification?>?>(null)
    val readNotificationsState = _readNotificationsState.asStateFlow()

    private val _unreadNotificationsState = MutableStateFlow<List<Notification?>?>(null)
    val unreadNotificationsState = _unreadNotificationsState.asStateFlow()

    private val _unreadNotificationsCount = MutableStateFlow<Int>(0)
    val unreadNotificationsCount = _unreadNotificationsCount.asStateFlow()

    fun fetchNotifications() {
        _notificationsState.value = NotificationsState.Loading
        var readNotifications: List<Notification?>? = null
        var unreadNotifications: List<Notification?>? = null
        var error: String? = null
        viewModelScope.launch {
            try {
                val notifications = User.fromId(
                    dataStoreManager.getUserId().first() ?: ""
                )?.notifications()
                readNotifications = notifications?.filter { it.read }
                unreadNotifications = notifications?.filter { !it.read }
                _unreadNotificationsCount.value = unreadNotifications?.size ?: 0
            } catch (e: Exception) {
                error = e.message
            }
            _readNotificationsState.value = readNotifications
            _unreadNotificationsState.value = unreadNotifications
            _notificationsState.value = NotificationsState.Idle
        }
    }

    fun markNotificationAsRead(notification: Notification) {
        viewModelScope.launch {
            if (!notification.read) {
                try {
                    notification.read = true // Set the notification as read
                    notification.update() // This calls the update method in the Notification model
                    Log.d("NotificationsViewModel", "Notification ${notification.id} marked as read.")
                    fetchNotifications() // Re-fetch to update UI and counts
                } catch (e: Exception) {
                    Log.e("NotificationsViewModel", "Error marking notification as read: ${e.message}", e)
                }
            }
        }
    }
}
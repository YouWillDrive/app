package ru.gd_alt.youwilldrive.ui.screens.Login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.data.DataStoreManager

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
}

class LoginViewModel(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun login(phone: String, password: String, onResponse: (User?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var user: User? = null
            var error: String? = null
            try {
                _loginState.value = LoginState.Loading
                Log.d("LoginViewModel", "Loading...")
                user = User.authorize(null, phone, password)
                if (user != null) {
                    Log.d("LoginViewModel", "Login successful: $user")
                    _loginState.value = LoginState.Idle
                    dataStoreManager.saveUserId(user.id)
                    Log.d("LoginViewModel", "User ID saved: ${user.id}")
                    Log.d("LoginViewModel", "User ID: ${dataStoreManager.getUserId()}")
                } else {
                    Log.d("LoginViewModel", "Login failed: Invalid credentials")
                    error = R.string.invalid_credentials.toString()
                    _loginState.value = LoginState.Idle
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login error", e)
                error = e.message ?: "Unknown error"
                _loginState.value = LoginState.Idle
            }
            withContext(Dispatchers.Main) {
                onResponse(user, error)
            }
        }
    }
}
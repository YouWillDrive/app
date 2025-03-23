package ru.gd_alt.youwilldrive.ui.screens.Login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.gd_alt.youwilldrive.models.User

class LoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                val user = User.login(email, password)
                if (user != null) {
                    Log.d("LoginViewModel", "Login successful: $user")
                    _loginState.value = LoginState.Success(user)
                } else {
                    Log.d("LoginViewModel", "Login failed: Invalid credentials")
                    _loginState.value = LoginState.Error("Invalid credentials")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login error", e)
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
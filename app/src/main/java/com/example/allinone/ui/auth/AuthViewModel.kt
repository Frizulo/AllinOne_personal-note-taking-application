package com.example.allinone.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.repo.AuthRepository
import com.example.allinone.worker.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username


    fun login(name: String, password: String, appContext : Context, onSuccess: () -> Unit) {
        _username.value = name
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { repo.login(name, password) }
                .onSuccess {
                    _uiState.value = AuthUiState()
                    onSuccess()
                    //Sync
                    SyncScheduler.enqueueOneTimeSync(appContext)
                    SyncScheduler.enqueuePeriodicSync(appContext)
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState(error = e.message ?: "Login failed")
                }
        }
    }

    fun register(name: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { repo.register(name, password) }
                .onSuccess {
                    _uiState.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState(error = e.message ?: "Register failed")
                }
        }
    }
}

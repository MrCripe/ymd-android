package com.mrcriper.ymd.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcriper.ymd.data.repository.AuthRepository
import com.mrcriper.ymd.di.TokenHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val login: String = "",
    val token: String = "",
    val accounts: List<String> = emptyList(),
    val activeAccount: String? = null,
    val message: String? = null,
    val verified: Boolean? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenHolder: TokenHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState(
            accounts = authRepository.listAccounts().toList(),
            activeAccount = authRepository.activeAccount.value,
        ),
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setLogin(value: String) = _state.update { it.copy(login = value) }
    fun setToken(value: String) = _state.update { it.copy(token = value, verified = null) }

    fun save() {
        val s = _state.value
        if (s.login.isBlank() || s.token.isBlank()) {
            _state.update { it.copy(message = "Login and token required") }
            return
        }
        viewModelScope.launch {
            val key = s.login
            authRepository.saveToken(key, s.token)
            tokenHolder.current = s.token
            _state.update {
                it.copy(
                    accounts = authRepository.listAccounts().toList(),
                    activeAccount = key,
                    token = "",
                    message = "Saved",
                    verified = true,
                )
            }
        }
    }

    fun verify() {
        val s = _state.value
        val token = s.token.ifBlank { tokenHolder.current }
        _state.update { it.copy(verified = !token.isNullOrBlank(), message = if (token.isNullOrBlank()) "No token" else null) }
    }

    fun selectAccount(key: String) {
        authRepository.selectAccount(key)
        tokenHolder.current = authRepository.getToken(key)
        _state.update { it.copy(activeAccount = key) }
    }

    fun delete(key: String) {
        authRepository.deleteAccount(key)
        _state.update {
            it.copy(
                accounts = authRepository.listAccounts().toList(),
                activeAccount = authRepository.activeAccount.value,
            )
        }
    }
}

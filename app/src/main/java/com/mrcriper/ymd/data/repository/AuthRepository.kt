package com.mrcriper.ymd.data.repository

import com.mrcriper.ymd.data.local.security.CryptoManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AuthRepository @Inject constructor(
    private val cryptoManager: CryptoManager,
) {

    private val _activeAccount = MutableStateFlow<String?>(null)
    val activeAccount: StateFlow<String?> = _activeAccount.asStateFlow()

    fun listAccounts(): Set<String> = cryptoManager.allAccounts()

    fun saveToken(accountKey: String, token: String) {
        cryptoManager.putToken(accountKey, token)
    }

    fun getToken(accountKey: String): String? = cryptoManager.getToken(accountKey)

    fun deleteAccount(accountKey: String) {
        cryptoManager.removeToken(accountKey)
        if (_activeAccount.value == accountKey) _activeAccount.value = null
    }

    fun selectAccount(accountKey: String) {
        _activeAccount.value = accountKey
    }

    fun currentToken(): String? = _activeAccount.value?.let { cryptoManager.getToken(it) }
}

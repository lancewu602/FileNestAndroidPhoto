package com.filenest.photo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.api.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            AppPrefKeys.getServerToken(context).collect { token ->
                _isLoggedIn.value = token.isNotBlank()
                if (token.isNotBlank()) {
                    val storedUrl = AppPrefKeys.getServerUrl(context).first()
                    if (storedUrl.isNotBlank()) {
                        retrofitClient.setServerUrl(storedUrl)
                    }
                }
            }
        }
    }

    fun login(serverUrl: String, username: String, password: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val mockToken = "mock_token_${java.util.UUID.randomUUID()}"
                AppPrefKeys.setServerUrl(context, serverUrl)
                AppPrefKeys.setUsername(context, username)
                AppPrefKeys.setServerToken(context, mockToken)
                retrofitClient.setServerUrl(serverUrl)
                _isLoggedIn.value = true
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                AppPrefKeys.setServerUrl(context, "")
                AppPrefKeys.setUsername(context, "")
                AppPrefKeys.setServerToken(context, "")
                _isLoggedIn.value = false
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            }
        }
    }

    fun getServerUrl() = AppPrefKeys.getServerUrl(context)

    fun getUsername() = AppPrefKeys.getUsername(context)
}
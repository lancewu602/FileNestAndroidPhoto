package com.filenest.photo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.api.LoginRequest
import com.filenest.photo.data.api.RetrofitClient
import com.filenest.photo.data.api.isRetOk
import com.filenest.photo.data.api.retMsg
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

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

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
                _loginError.value = null
                retrofitClient.setServerUrl(serverUrl)
                val api = retrofitClient.getApiService()
                val response = api.login(LoginRequest(username, password))

                if (isRetOk(response)) {
                    val token = response.data?.token
                    if (token.isNullOrBlank()) {
                        _loginError.value = "登录失败：未获取到token"
                    } else {
                        AppPrefKeys.setServerUrl(context, serverUrl)
                        AppPrefKeys.setUsername(context, username)
                        AppPrefKeys.setServerToken(context, token)
                        _isLoggedIn.value = true
                        onComplete()
                    }
                } else {
                    _loginError.value = retMsg(response)
                }
            } catch (e: Exception) {
                _loginError.value = e.message ?: "网络错误"
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
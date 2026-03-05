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
class WelcomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val token = AppPrefKeys.getServerToken(context).first()
            _isLoggedIn.value = token.isNotBlank()
            _isCheckingAuth.value = false
            if (token.isNotBlank()) {
                val storedUrl = AppPrefKeys.getServerUrl(context).first()
                if (storedUrl.isNotBlank()) {
                    retrofitClient.setServerUrl(storedUrl)
                }
            }
        }
    }
}
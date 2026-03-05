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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    fun getServerUrl() = AppPrefKeys.getServerUrl(context)

    fun getUsername() = AppPrefKeys.getUsername(context)

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoggingOut.value = true
                AppPrefKeys.setServerUrl(context, "")
                AppPrefKeys.setUsername(context, "")
                AppPrefKeys.setServerToken(context, "")
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete()
            } finally {
                _isLoggingOut.value = false
            }
        }
    }
}
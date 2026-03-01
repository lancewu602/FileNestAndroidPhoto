package com.filenest.photo.viewmodel

import androidx.lifecycle.ViewModel
import com.filenest.photo.data.api.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {


}
package com.filenest.photo.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

}

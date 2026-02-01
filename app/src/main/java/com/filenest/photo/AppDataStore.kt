package com.filenest.photo

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "app_prefs_test_100")

object AppPrefKeys {


}

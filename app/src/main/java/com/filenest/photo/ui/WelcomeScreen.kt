package com.filenest.photo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.filenest.photo.viewmodel.WelcomeViewModel

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToBrowse: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()

    LaunchedEffect(isCheckingAuth, isLoggedIn) {
        if (!isCheckingAuth) {
            if (isLoggedIn) {
                onNavigateToBrowse()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "File Nest",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
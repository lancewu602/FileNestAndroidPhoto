package com.filenest.photo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.filenest.photo.theme.MainTheme
import com.filenest.photo.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionsToRequest = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val permissionChecker = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            showPermissionDialog = true
        }
    }

    private var showPermissionDialog by mutableStateOf(false)

    private fun checkAndRequestPermissions() {
        val permissionsToCheck = permissionsToRequest.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToCheck.isEmpty()) {
            return
        }

        val shouldShowRationale = permissionsToCheck.any {
            shouldShowRequestPermissionRationale(it)
        }

        if (shouldShowRationale) {
            showPermissionDialog = true
        } else {
            permissionChecker.launch(permissionsToCheck)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestPermissions()

        setContent {
            MainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showPermissionDialog = false },
                            title = { Text("权限申请") },
                            text = { Text("为了提供更好的服务，请授予以下权限：\n\n- 访问媒体资源：用于备份和同步您的照片\n- 通知权限：用于接收同步状态和重要提醒") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showPermissionDialog = false
                                        permissionLauncher.launch(permissionsToRequest)
                                    }
                                ) {
                                    Text("同意")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showPermissionDialog = false }
                                ) {
                                    Text("取消")
                                }
                            }
                        )
                    }

                    MainScreen()
                }
            }
        }
    }
}
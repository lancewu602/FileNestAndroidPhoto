# 权限申请弹窗实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在 App 启动时顺序申请通知和媒体资源权限，使用 Activity Result API 处理权限回调，DataStore 记录申请状态。

**Architecture:** 在 MainActivity 中使用 registerForActivityResult 注册权限请求，LaunchedEffect 监听 DataStore 状态并触发申请，顺序申请 POST_NOTIFICATIONS 和 READ_MEDIA_IMAGES。

**Tech Stack:** Activity Result API, Jetpack Compose, DataStore

---

### Task 1: 添加通知权限声明

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:4`

**Step 1: 在 uses-permission 中添加 POST_NOTIFICATIONS 权限**

在 READ_MEDIA_IMAGES 权限之前添加：
```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

完整修改后的 AndroidManifest.xml 第 1-6 行：
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

**Step 2: 提交修改**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add POST_NOTIFICATIONS permission"
```

---

### Task 2: 添加权限状态Key到 PreferencesKeys

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/data/AppDataStore.kt`

**Step 1: 在 PreferencesKeys object 中添加权限状态键**

找到 `object PreferencesKeys`，在现有键之后添加：
```kotlin
    val hasRequestedNotificationPermission = booleanPreferencesKey("has_requested_notification_permission")
    val hasRequestedMediaPermission = booleanPreferencesKey("has_requested_media_permission")
```

完整示例（根据实际代码调整）：
```kotlin
private object PreferencesKeys {
    val hasRequestedNotificationPermission = booleanPreferencesKey("has_requested_notification_permission")
    val hasRequestedMediaPermission = booleanPreferencesKey("has_requested_media_permission")
}
```

**Step 2: 提交修改**

```bash
git add app/src/main/java/com/filenest/photo/data/AppDataStore.kt
git commit -m "feat: add permission status keys to DataStore"
```

---

### Task 3: 为 Context 添加扩展属性读取权限状态

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/data/AppDataStore.kt`

**Step 1: 在文件末尾添加扩展属性**

如果已有类似扩展属性（如 `username`），在相同区域添加：
```kotlin
val Context.hasRequestedNotificationPermission: Flow<Boolean>
    get() = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.hasRequestedNotificationPermission] ?: false
    }

val Context.hasRequestedMediaPermission: Flow<Boolean>
    get() = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.hasRequestedMediaPermission] ?: false
    }
```

**Step 2: 提交修改**

```bash
git add app/src/main/java/com/filenest/photo/data/AppDataStore.kt
git commit -m "feat: add permission status extension properties"
```

---

### Task 4: 为 Context 添加保存权限状态的suspend函数

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/data/AppDataStore.kt`

**Step 1: 在文件中添加保存函数**

如果已有类似 suspend 函数（如 `saveUsername`），在相同区域添加：
```kotlin
suspend fun Context.saveHasRequestedNotificationPermission(hasRequested: Boolean) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.hasRequestedNotificationPermission] = hasRequested
    }
}

suspend fun Context.saveHasRequestedMediaPermission(hasRequested: Boolean) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.hasRequestedMediaPermission] = hasRequested
    }
}
```

**Step 2: 提交修改**

```bash
git add app/src/main/java/com/filenest/photo/data/AppDataStore.kt
git commit -m "feat: add permission status save functions"
```

---

### Task 5: 在 MainActivity 中添加权限请求代码

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/MainActivity.kt`

**Step 1: 添加必要的导入**

在导入区域添加：
```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
```

**Step 2: 修改 onCreate 方法实现权限申请逻辑**

完整替换 `MainActivity.kt` 内容：
```kotlin
package com.filenest.photo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.filenest.photo.theme.MainTheme
import com.filenest.photo.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestNotificationPermissionLauncher = 
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            lifecycleScope.launch {
                saveHasRequestedNotificationPermission(true)
                shouldRequestMediaPermission.value = true
            }
        }

    private val requestMediaPermissionLauncher = 
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            lifecycleScope.launch {
                saveHasRequestedMediaPermission(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MainTheme {
                val context = LocalContext.current
                var shouldRequestNotificationPermission by remember { mutableStateOf(false) }
                var shouldRequestMediaPermission by remember { mutableStateOf(false) }

                val hasRequestedNotificationPermission by context.hasRequestedNotificationPermission
                    .collectAsState(initial = false)
                val hasRequestedMediaPermission by context.hasRequestedMediaPermission
                    .collectAsState(initial = false)

                LaunchedEffect(Unit) {
                    shouldRequestNotificationPermission = !hasRequestedNotificationPermission
                }

                LaunchedEffect(shouldRequestNotificationPermission) {
                    if (shouldRequestNotificationPermission) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            lifecycleScope.launch {
                                saveHasRequestedNotificationPermission(true)
                            }
                        }
                        shouldRequestNotificationPermission = false
                    }
                }

                LaunchedEffect(shouldRequestMediaPermission) {
                    if (shouldRequestMediaPermission) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.READ_MEDIA_IMAGES
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestMediaPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            lifecycleScope.launch {
                                saveHasRequestedMediaPermission(true)
                            }
                        }
                        shouldRequestMediaPermission = false
                    }
                }

                MainScreen()
            }
        }
    }
}
```

**Step 3: 提交修改**

```bash
git add app/src/main/java/com/filenest/photo/MainActivity.kt
git commit -m "feat: implement permission request on app startup"
```

---

### Task 6: 验证构建

**Files:**
- Build: entire project

**Step 1: 运行构建**

```bash
./gradlew clean build
```

验证输出：BUILD SUCCESSFUL

**Step 2: 如果构建失败，运行 lint 检查**

```bash
./gradlew lint
```

解决 lint 错误后重新构建

**Step 7: 最终提交（如需要）**

如果 Task 6 中有任何修复，提交修复：

```bash
git add .
git commit -m "fix: resolve build issues"
```

# 相册同步配置功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 添加相册同步配置页面，允许用户从手机相册列表中选择需要同步的相册。

**架构:** 使用 Material3 + Compose 构建 UI，DataStore Preferences 存储同步配置，MediaStore 读取系统相册，ViewModel 管理状态。

**技术栈:** Jetpack Compose, Material3, DataStore, MediaStore, Navigation Compose, Hilt, Coroutines

---

### Task 1: 添加路由定义

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/ui/MainScreen.kt:13-18`

**Step 1: 在 Screen sealed class 中添加新路由**

在 `Screen` sealed class 中添加 `AlbumSync` 对象：

```kotlin
sealed class Screen(val route: String, val title: String) {
    data object Login : Screen("login", "登录")
    data object Browse : Screen("browse", "浏览")
    data object Sync : Screen("sync", "同步")
    data object Settings : Screen("settings", "设置")
    data object AlbumSync : Screen("album_sync", "配置同步相册")
}
```

**Step 2: 在 NavHost 中添加路由定义**

在 `MainScreen` 的 NavHost 中添加 `album_sync` 路由（在 `composable(Screen.Settings.route)` 之后）：

```kotlin
composable(Screen.AlbumSync.route) { AlbumSyncScreen(navController) }
```

**Step 3: 编译检查**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add app/src/main/java/com/filenest/photo/ui/MainScreen.kt
git commit -m "feat(routes): add AlbumSync screen route"
```

### Task 2: 创建 AlbumData 数据类

**Files:**
- Create: `app/src/main/java/com/filenest/photo/data/AlbumData.kt`

**Step 1: 创建数据类文件**

```kotlin
package com.filenest.photo.data

data class AlbumData(
    val bucketId: Long,
    val bucketName: String
)
```

**Step 2: 提交**

```bash
git add app/src/main/java/com/filenest/photo/data/AlbumData.kt
git commit -m "feat(data): add AlbumData model"
```

### Task 3: 扩展 DataStore 存储同步配置

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/data/AppDataStore.kt`

**Step 1: 添加导入和键定义**

在文件顶部的 import 区域添加：

```kotlin
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
```

在 `AppPrefKeys` object 中添加：

```kotlin
// 已选择的相册 bucketId 列表（JSON 格式）
val SELECTED_ALBUMS = stringPreferencesKey("selected_albums")
```

**Step 2: 添加获取和设置方法**

在 `AppPrefKeys` object 中添加：

```kotlin
fun getSelectedAlbums(context: Context): Flow<Set<Long>> {
    return context.dataStore.data.map { settings ->
        val jsonString = settings[SELECTED_ALBUMS] ?: return@map emptySet()
        try {
            Json.decodeFromString<List<Long>>(jsonString).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}

suspend fun setSelectedAlbums(context: Context, albums: Set<Long>) {
    context.dataStore.edit { settings ->
        settings[SELECTED_ALBUMS] = Json.encodeToString(albums.toList())
    }
}
```

**Step 3: 编译检查**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add app/src/main/java/com/filenest/photo/data/AppDataStore.kt
git commit -m "feat(datastore): add selected albums storage"
```

### Task 4: 扩展 ViewModel 添加相册功能

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/viewmodel/MainViewModel.kt`

**Step 1: 添加相册状态**

在类顶部添加状态管理：

```kotlin
private val _selectedAlbums = MutableStateFlow<Set<Long>>(emptySet())
val selectedAlbums: StateFlow<Set<Long>> = _selectedAlbums.asStateFlow()

private val _albums = MutableStateFlow<List<AlbumData>>(emptyList())
val albums: StateFlow<List<AlbumData>> = _albums.asStateFlow()
```

**Step 2: 在 init 方法中加载同步配置**

在现有的 `init` block 中添加：

```kotlin
init {
    viewModelScope.launch {
        getUsername(context).collect { _username.value = it }
        isLoggedIn.collect { _isLoggedIn.value = it }
        getSelectedAlbums(context).collect { _selectedAlbums.value = it }
    }
}
```

**Step 3: 添加读取系统相册方法**

添加方法：

```kotlin
fun loadAlbums() {
    viewModelScope.launch {
        val albums = try {
            val projection = arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val albumMap = mutableMapOf<Long, String>()
            
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                
                while (cursor.moveToNext()) {
                    val bucketId = cursor.getLong(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn)
                    albumMap[bucketId] = bucketName
                }
            }
            
            albumMap.map { (bucketId, bucketName) ->
                AlbumData(bucketId, bucketName)
            }.sortedBy { it.bucketName }
        } catch (e: Exception) {
            emptyList()
        }
        _albums.value = albums
    }
}
```

**Step 4: 添加切换相册方法**

添加方法：

```kotlin
fun toggleAlbum(bucketId: Long) {
    viewModelScope.launch {
        val current = _selectedAlbums.value.toMutableSet()
        if (current.contains(bucketId)) {
            current.remove(bucketId)
        } else {
            current.add(bucketId)
        }
        _selectedAlbums.value = current
        setSelectedAlbums(context, current)
    }
}
```

**Step 5: 编译检查**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: 提交**

```bash
git add app/src/main/java/com/filenest/photo/viewmodel/MainViewModel.kt
git commit -m "feat(viewmodel): add album sync methods"
```

### Task 5: 创建 AlbumSyncScreen UI

**Files:**
- Create: `app/src/main/java/com/filenest/photo/ui/AlbumSyncScreen.kt`

**Step 1: 创建页面文件**

```kotlin
package com.filenest.photo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.filenest.photo.data.AlbumData
import com.filenest.photo.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSyncScreen(navController: androidx.navigation.NavHostController) {
    val viewModel: MainViewModel = hiltViewModel()
    val albums by viewModel.albums.collectAsState()
    val selectedAlbums by viewModel.selectedAlbums.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadAlbums()
        }
    }

    androidx.compose.material3.LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                androidx.compose.ui.platform.LocalContext.current,
                Manifest.permission.READ_MEDIA_IMAGES
            )
            if (status == PackageManager.PERMISSION_GRANTED) {
                viewModel.loadAlbums()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            viewModel.loadAlbums()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置同步相册") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material.icons.Icons.Default.ArrowBack.let { icon ->
                            androidx.compose.material3.Icon(icon, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(albums) { album ->
                AlbumItem(
                    album = album,
                    isSelected = album.bucketId in selectedAlbums,
                    onToggle = { viewModel.toggleAlbum(album.bucketId) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun AlbumItem(
    album: AlbumData,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = album.bucketName,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
    }
}
```

**Step 2: 编译检查**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add app/src/main/java/com/filenest/photo/ui/AlbumSyncScreen.kt
git commit -m "feat(ui): create album sync screen"
```

### Task 6: 在设置页面添加跳转菜单

**Files:**
- Modify: `app/src/main/java/com/filenest/photo/ui/SettingScreen.kt`

**Step 1: 添加导入**

在 import 区域添加：

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
```

**Step 2: 在用户名和配置同步相册之间添加菜单项**

在 `TextContentPair(title = "用户名", content = username)` 后添加：

```kotlin
HorizontalDivider()
androidx.compose.foundation.layout.Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { navController.navigate("album_sync") }
        .padding(16.dp),
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
) {
    Text("配置同步相册", style = MaterialTheme.typography.bodyMedium)
    Icon(Icons.Default.ChevronRight, contentDescription = null)
}
HorizontalDivider()
```

**Step 3: 编译检查**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add app/src/main/java/com/filenest/photo/ui/SettingScreen.kt
git commit -m "feat(settings): add album sync menu item"
```

### Task 7: 添加运行时权限

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: 在 <manifest> 标签下添加权限**

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

**Step 2: 编译检查**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(permissions): add READ_MEDIA_IMAGES permission"
```

### Task 8: 添加 kotlinx-serialization 依赖

**Files:**
- Modify: `app/build.gradle.kts`

**Step 1: 检查 serialization 插件**

确认文件中有：
```kotlin
kotlin("plugin.serialization")
```

**Step 2: 添加 kotlinx-serialization 依赖**

在 dependencies 块中添加（如果没有）：
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
```

**Step 3: 同步项目**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 4: 如果依赖已存在则跳过此任务**

检查文件中是否已有 kotlinx-serialization-json 依赖。

---

## 最终验证

**Step 1: 完整构建**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: 运行应用**

Run: `./gradlew installDebug`
Expected: INSTALL SUCCESSFUL

**Step 3: 手动测试流程**

1. 打开应用
2. 进入设置页面
3. 点击"配置同步相册"
4. 授权相册访问权限
5. 查看相册列表
6. 切换几个相册的开关
7. 返回设置页面
8. 再次进入"配置同步相册"
9. 验证开关状态保持

**Step 4: 最终提交**

```bash
git add docs/plans/2026-02-26-album-sync-design.md
git commit -m "docs: add album sync implementation plan"
```

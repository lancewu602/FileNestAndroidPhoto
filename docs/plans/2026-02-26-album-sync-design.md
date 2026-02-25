# 相册同步配置功能设计

## 概述
在设置页面添加"配置同步相册"菜单项，跳转到独立页面，展示手机中所有相册，允许用户控制哪些相册需要同步到服务器。

## 功能需求
1. 在设置页面"用户名"下方添加"配置同步相册"菜单项
2. 点击后跳转到独立的相册配置页面
3. 页面展示手机中所有相册列表
4. 每个相册一行显示：左侧为相册名称，右侧为开关按钮
5. 开关控制该相册是否参与同步

## 架构设计

### 路由结构
- 在 `Screen` sealed class 中添加：
  ```kotlin
  data object AlbumSync : Screen("album_sync", "配置同步相册")
  ```
- 在 `MainScreen` 的 NavHost 中添加 composable 路由定义

### UI 组件

#### AlbumSyncScreen.kt
- 使用 Scaffold 布局，包含 TopAppBar（返回按钮）
- 使用 LazyColumn 展示相册列表
- 每个相册使用 AlbumItem 组件渲染

#### AlbumItem Composable
- Row 布局，占据全宽，padding 16dp
- 左侧：Text 显示相册名称（style = MaterialTheme.typography.bodyMedium）
- 右侧：Switch 开关，绑定选中状态
- HorizontalDivider 分隔

#### SettingScreen.kt 修改
- 在 TextContentPair("用户名", username) 后添加菜单项
- 使用可点击的 Row，带 chevron 图标
- 点击跳转到 album_sync 路由

### 数据模型

#### AlbumData.kt
```kotlin
data class AlbumData(
    val bucketId: Long,      // 相册唯一标识
    val bucketName: String   // 相册名称
)
```

### 数据存储

#### AppDataStore.kt 扩展
添加：
- `SELECTED_ALBUMS` = stringPreferencesKey("selected_albums")
- 存储格式：JSON 数组字符串，包含选中的 bucketId 列表
- `getSelectedAlbums(context: Context): Flow<Set<Long>>`
- `setSelectedAlbums(context: Context, albums: Set<Long>)`

### ViewModel 扩展

#### MainViewModel.kt
添加方法：
- `getAlbums(): Flow<List<AlbumData>>` - 通过 ContentResolver 查询 MediaStore
- `getSelectedAlbums(): Flow<Set<Long>>` - 从 DataStore 读取
- `toggleAlbum(bucketId: Long)` - 切换相册同步状态

MediaStore 查询投影：
- `MediaStore.Images.Media.BUCKET_ID`
- `MediaStore.Images.Media.BUCKET_DISPLAY_NAME`
- 按 BUCKET_ID 分组，去重

### 权限处理
- 在 `AndroidManifest.xml` 添加：
  ```xml
  <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
  ```
- Android 13+ 使用 READ_MEDIA_IMAGES
- 点击页面时请求运行时权限（如果未授权）

## 数据流

1. **页面初始化**
   - AlbumSyncScreen 从 ViewModel 获取所有相册列表
   - 从 ViewModel 获取已选相册集合
   - 合并计算每个相册的开关状态

2. **用户交互**
   - 用户点击某个相册的开关
   - 触发 ViewModel.toggleAlbum()
   - ViewModel 更新已选相册集合
   - 保存到 DataStore
   - Flow 通知 UI 更新状态

3. **权限流程**
   - 检查权限状态
   - 未授权则请求 READ_MEDIA_IMAGES 权限
   - 授权后加载相册列表
   - 拒绝则显示权限提示

## 错误处理
- 存储失败：捕获异常并记录日志
- 权限被拒绝：显示提示，引导用户到设置页面
- MediaStore 查询异常：显示错误提示，提供重试按钮

## 测试要点
- 相册列表正确加载
- 开关状态正确反映同步配置
- 切换开关正确更新 DataStore
- 权限请求流程完整
- 页面返回后状态保持

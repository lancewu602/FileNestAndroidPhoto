# 权限申请弹窗设计文档

## 概述

在 App 启动时增加通知和媒体资源的权限申请弹窗，顺序申请 `POST_NOTIFICATIONS` 和 `READ_MEDIA_IMAGES` 权限。

## 需求

- 权限申请成功后记录状态，不再重复弹出
- 顺序先后申请权限：通知 → 媒体
- 允许用户拒绝，拒绝后功能受限但不阻止使用
- 在 MainActivity 中直接处理，不使用单独的 ViewModel

## 架构设计

### 技术方案

- **权限请求**: Activity Result API (`registerForActivityResult`)
- **状态持久化**: DataStore 记录是否已申请过权限
- **触发时机**: App 启动时检查 DataStore 状态，未申请则弹窗

### 权限流程

```
启动App
  ↓
检查DataStore是否已申请过
  ↓ 未申请
弹出通知权限对话框 (POST_NOTIFICATIONS)
  ↓
用户选择（同意/拒绝）
记录状态到DataStore，继续下一权限
  ↓
弹出媒体权限对话框 (READ_MEDIA_IMAGES)
  ↓
用户选择（同意/拒绝）
记录状态到DataStore，继续进入主界面
```

## 实现要点

### 1. AndroidManifest.xml

添加通知权限声明：
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 2. AppDataStore

添加权限申请状态键：
```kotlin
object PreferencesKeys {
    val HAS_REQUESTED_NOTIFICATION_PERMISSION = booleanPreferencesKey("has_requested_notification_permission")
    val HAS_REQUESTED_MEDIA_PERMISSION = booleanPreferencesKey("has_requested_media_permission")
}
```

### 3. MainActivity

- 使用 `registerForActivityResult(ActivityResultContracts.RequestPermission())` 注册权限回调
- 使用 `LaunchedEffect` 监听状态并触发权限申请
- 顺序申请：先通知，后媒体

## 文件清单

- `app/src/main/AndroidManifest.xml` - 添加权限声明
- `app/src/main/java/com/filenest/photo/data/AppDataStore.kt` - 添加权限状态键
- `app/src/main/java/com/filenest/photo/MainActivity.kt` - 实现权限申请逻辑

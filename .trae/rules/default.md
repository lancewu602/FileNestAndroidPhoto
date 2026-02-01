适用于：Android App（API 33+），Jetpack Compose + Material Design 3
目标：UI 美观、结构清晰、易维护、AI 友好

## 技术栈约定

* UI：Jetpack Compose + Material Design 3
* 架构：MVVM + 单向数据流（UDF）
* 异步：Kotlin Coroutines + Flow
* 依赖注入：Hilt
* 最低支持：Android API 33+

## 架构规则

* Composable 只负责渲染 UI，不包含业务逻辑
* ViewModel：
  * 只对外暴露 `StateFlow<UiState>`
  * 通过 `onEvent(event)` 处理用户行为
* UseCase 禁止在 UI 层直接调用，仅在 ViewModel 中调用，可以直接访问 dao 及 api
* dao 仅用于数据库操作，不包含业务逻辑
* api 仅用于网络请求，不包含业务逻辑

## Compose 编码规范

* Composable 必须小而可组合，单一职责
* 参数顺序统一：
  ```kotlin
  modifier → state → callbacks
  ```
* 协程仅允许在 `ViewModel` 或 `LaunchedEffect` 中使用

## UI & 设计规范

* 仅使用 Material 3 组件
* 禁止硬编码：颜色 / 字号 / 圆角
* 间距统一使用 `dp` 常量或 `MaterialTheme`
* 必须支持 深色模式（Dark Theme）

## 状态 & 副作用

* UI 状态：`data class XxxUiState`
* 网络 / IO 操作仅存在于 ViewModel / UseCase

## 命名规范

* Composable：`LoginScreen`, `UserCard`
* ViewModel：`LoginViewModel`
* Dao：`UserDao`
* Api：`UserApi`
* State：`LoginUiState`
* Event：`LoginEvent`

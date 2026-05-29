# foundation-core

`foundation-core` 是 Android App 开发脚手架基础库，提供可直接复用的基础设施和通用 UI 能力。

## 基本信息

- Gradle module：`:foundation-core`
- Namespace：`com.holderzone.foundation.core`
- Artifact 类型：Android Library AAR
- 最低 SDK：由根工程 `libs.versions.toml` 中的 `minSdk` 控制
- 主要依赖：AndroidX、Kotlin Coroutines、Navigation、OkHttp、Retrofit、MMKV

## 能力目录

```text
com.holderzone.foundation.core
├── app
├── network
├── navigation
├── platform
│   ├── file
│   ├── logging
│   ├── network
│   └── storage
└── ui
    ├── base
    ├── feedback
    └── widget
```

## 网络层

网络层提供 HTTP 基础设施：

- `NetworkOptions`
- `NetworkManager`
- `HttpLogInterceptor`
- `BaseUrlManager`
- `ServiceFactory`
- Retrofit 创建与缓存
- OkHttp 创建与配置入口
- 动态 BaseUrl 切换

默认策略：

- 由接入方按需添加 interceptor。
- 由接入方按需添加 converter。
- 由接入方按需添加 call adapter。
- 不固定 converter 或 call adapter。
- 默认不信任所有证书。
- 默认不打印 BODY 日志。

### 使用示例

```kotlin
val networkManager = NetworkManager(
    NetworkOptions(
        defaultBaseUrl = "https://api.example.com",
        connectTimeoutSeconds = 15,
        readTimeoutSeconds = 15,
        writeTimeoutSeconds = 15,
        logLevel = HttpLogLevel.BASIC,
        okHttpConfig = {
            addInterceptor(appInterceptor)
        },
        retrofitConfig = {
            addConverterFactory(appConverterFactory)
        },
    ),
)

val api = networkManager.create(ApiService::class.java)
```

切换 BaseUrl：

```kotlin
networkManager.setBaseUrl("https://backup.example.com")
networkManager.clearCache("https://api.example.com")
```

如需 HTTP 日志，宿主 App 自行决定是否接入 `HttpLogInterceptor`。该拦截器会把一次 request、一次 response 分别聚合为完整字符串后再输出，避免日志框架按行打印时出现重复边框或复制混乱：

```kotlin
val networkManager = NetworkManager(
    NetworkOptions(
        defaultBaseUrl = "https://api.example.com",
        okHttpConfig = {
            addInterceptor(
                HttpLogInterceptor(
                    logger = HttpLogInterceptor.Logger { message ->
                        AppLogger.t("HttpLog").i(message)
                    },
                ).apply {
                    level = HttpLogInterceptor.Level.BODY
                    redactHeaders("Authorization", "Cookie", "Set-Cookie")
                },
            )
        },
    ),
)
```

## 导航

导航能力使用事件驱动方式封装：

- `AppNavigator`
- `NavigationEvent`
- `NavController.handleNavigationEvent(...)`
- 返回结果传递能力

示例：

```kotlin
viewModel.navigateTo("sample/detail")
viewModel.navigateBack(mapOf("updated" to true))
```

宿主 App 需要在合适位置收集 `AppNavigator.navigationEvents`，并交给 `NavController` 处理。

## UI 基类

基础 UI 能力包括：

- `BaseApplication`
- `BaseActivity`
- `BaseFragment`
- `ViewModelFragment`
- `BaseDialogFragment`
- `BaseViewModel`
- `BaseNetworkViewModel`

这些类处理 foundation 工具初始化、可选沉浸式系统栏、可选默认字体缩放、ViewBinding 生命周期、ViewModel 创建、导航事件、通用 loading/toast 触发。

`BaseApplication` 默认初始化 `MMKVUtils` 与 `StringResHelper`。如宿主 App 不需要其中某项，可覆盖对应开关：

```kotlin
class SampleApplication : BaseApplication() {
    override val initializeMMKV = false
}
```

`BaseActivity` 提供通用的系统栏、字体缩放、PermissionX 权限申请和软键盘辅助能力：

```kotlin
class MainActivity : BaseActivity() {
    override val hideSystemBarsOnCreate = true
    override val forceDefaultFontScale = true
}
```

## 反馈组件

反馈能力包括：

- `Toaster`
- `ToastEvent`
- `ToastLoadingHost`
- `StyledToast`
- `ConfirmDialogFragment`
- `StateViewBinder`

宿主 App 可以按自己的页面结构接入 loading overlay、toast 事件收集和确认弹窗结果处理。

## 通用控件

当前包含：

- `InputField`
- `DropdownField`
- `TitleBar`
- `NumericKeypadView`
- `DateRangePickerDialogFragment`
- `DateTimePickerDialogFragment`

控件提供基础交互与可配置入口。

## 平台工具

当前包含：

- `MMKVUtils`
- `StringResHelper`
- `AppLogger`
- `LoggerInitConfig`
- `NetworkMonitor`
- `FileDeleteQueue`
- `SoftKeyboardUtils`

如宿主 App 未继承 `BaseApplication`，使用 MMKV 前需要手动初始化：

```kotlin
MMKVUtils.init(applicationContext)
```

如宿主 App 未继承 `BaseApplication`，使用 `StringResHelper` 前需要手动初始化：

```kotlin
StringResHelper.initialize(applicationContext)
```

`BaseActivity` 内置了 PermissionX 权限申请封装、导航栏隐藏监听和软键盘工具接入。默认权限集合包含相机、图片/外部存储、网络权限；宿主 App 也可以传入自己的权限列表：

```kotlin
getPermission(listOf(Manifest.permission.CAMERA))
```

## 测试与构建

```bash
./gradlew :foundation-core:testDebugUnitTest
./gradlew :foundation-core:assembleDebug
./gradlew :foundation-core:assembleRelease
```

debug AAR 不开启混淆，适合本地调试；release AAR 开启 R8 混淆，适合集成验证或交付。

```text
foundation-core/build/outputs/aar/foundation-core-debug.aar
foundation-core/build/outputs/aar/foundation-core-release.aar
```

`release` 构建使用 `proguard-rules.pro` 处理库自身混淆，并通过 `consumer-rules.pro` 向引用方传递必要 keep 规则。

当前单元测试覆盖：

- BaseUrl 规范化
- BaseUrl 清空与非法协议校验
- 无 BaseUrl 创建服务时的错误提示
- Retrofit 缓存复用
- Retrofit 缓存清理
- 自定义 OkHttp interceptor 生效
- 自定义 Retrofit converter 生效
- 默认不添加 BODY 日志

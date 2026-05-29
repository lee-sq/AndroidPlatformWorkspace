# AndroidPlatformWorkspace

`AndroidPlatformWorkspace` 是 HolderZone Android 通用能力的大仓，用于统一管理可复用的 Android library module。当前首个 module 是 `:foundation-core`，后续可以继续在同一仓库下增加新的独立能力模块。

## 工程定位

- 这是一个 Android Gradle 多 module 工程。
- 每个 module 都应产出可复用的 Android library AAR。
- 大仓只沉淀跨 App 复用能力。
- 类名使用语义化命名，能力边界通过包名区分。

## 当前模块

| Module | Namespace | 说明 |
| --- | --- | --- |
| `:foundation-core` | `com.holderzone.foundation.core` | Android App 基础脚手架能力，包括应用/页面基类、网络基础设施、导航事件、基础 Fragment/ViewModel、反馈组件、通用控件和平台工具。 |

## 目录结构

```text
AndroidPlatformWorkspace/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── foundation-core/
```

## 新增模块约定

1. 在根目录创建新的 Android library module。
2. 在 `settings.gradle.kts` 中添加 `include(":module-name")`。
3. module namespace 使用 `com.holderzone.foundation.*` 或更具体的能力域包名。
4. 公共 API 保持语义化命名。
5. 新模块应保持清晰的能力边界，并提供独立的构建与测试入口。

## 构建与验证

```bash
./gradlew :foundation-core:testDebugUnitTest
./gradlew :foundation-core:assembleDebug
```

构建成功后，debug AAR 输出在：

```text
foundation-core/build/outputs/aar/foundation-core-debug.aar
```

## 能力范围

本仓库适合沉淀：

- Android 平台基础能力
- 网络基础设施
- 通用应用、页面基类与轻量控件
- 跨 App 复用的工具类
- 可独立测试、独立发布的 library module

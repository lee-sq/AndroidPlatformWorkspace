# AndroidPlatformWorkspace

`AndroidPlatformWorkspace` 是 HolderZone Android 通用能力的大仓，用于统一管理可复用的 Android library module。当前包含 `:foundation-core` 和 `device` 设备能力包，后续可以继续在同一仓库下增加新的独立能力模块。

## 工程定位

- 这是一个 Android Gradle 多 module 工程。
- 每个 module 都应产出可复用的 Android library AAR。
- 大仓只沉淀跨 App 复用能力。
- 类名使用语义化命名，能力边界通过包名区分。

## 当前模块

| Module | Namespace | 说明 |
| --- | --- | --- |
| `:foundation-core` | `com.holderzone.foundation.core` | Android App 基础脚手架能力，包括应用/页面基类、网络基础设施、导航事件、基础 Fragment/ViewModel、反馈组件、通用控件和平台工具。 |
| `:device-api-base` | `com.holderzone.device.api.base` | 设备通用 API、设备模型、串口配置和驱动策略接口。 |
| `:device-api-scale` | `com.holderzone.device.api.scale` | 称重设备 API 和能力接口。 |
| `:device-api-cabinet` | `com.holderzone.device.api.cabinet` | 柜体、柜门、温控、打印等设备 API 和能力接口。 |
| `:device-core` | `com.holderzone.device.core` | 设备注册、探测、串口通道、运行时读写循环和重连主链路。 |
| `:device-driver-scale` | `com.holderzone.device.driver.scale` | 称重设备驱动实现。 |
| `:device-driver-cabinet` | `com.holderzone.device.driver.cabinet` | 柜体设备驱动实现，包含厂商 jar 和 native so。 |
| `:device-starter-scale` | `com.holderzone.device.starter.scale` | 称重设备推荐接入入口，聚合称重 API、core 和驱动。 |
| `:device-starter-cabinet` | `com.holderzone.device.starter.cabinet` | 柜体设备推荐接入入口，聚合柜体 API、core 和驱动。 |

## 目录结构

```text
AndroidPlatformWorkspace/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── foundation-core/
└── device/
    ├── api/
    ├── core/
    ├── driver/
    └── starter/
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
./gradlew :device-core:testDebugUnitTest
./gradlew :device-starter-scale:assembleRelease
./gradlew :device-starter-cabinet:assembleRelease
```

构建成功后，debug AAR 输出在：

```text
foundation-core/build/outputs/aar/foundation-core-debug.aar
```

## JitPack 发布约定

本仓库按多 library module 管理，每个 module 独立发版。不要使用全仓统一的 `vX.Y.Z` tag，统一使用 module-scoped tag：

```text
<module-name>-v<semver>
```

当前 `foundation-core` 的 tag 示例：

```bash
git tag foundation-core-v0.1.0
git push origin foundation-core-v0.1.0
```

`device` 包按领域拆成多个 Gradle module，但作为一个设备能力包统一发版，tag 示例：

```bash
git tag device-v0.1.0
git push origin device-v0.1.0
```

`gradle.properties` 中的 `*.version` 仅作为本地发布默认版本。JitPack 构建时使用 Git tag 作为最终依赖版本。

JitPack 会根据 tag 前缀只发布对应 module。当前支持：

```text
foundation-core-v* -> :foundation-core
device-v*          -> :device-api-base, :device-api-scale, :device-api-cabinet,
                      :device-core, :device-driver-scale, :device-driver-cabinet,
                      :device-starter-scale, :device-starter-cabinet
```

发布前建议本地验证：

```bash
VERSION=foundation-core-v0.1.0 ./gradlew :foundation-core:testDebugUnitTest :foundation-core:publishReleasePublicationToMavenLocal
VERSION=device-v0.1.0 ./gradlew \
  :device-api-base:testDebugUnitTest \
  :device-api-scale:testDebugUnitTest \
  :device-api-cabinet:testDebugUnitTest \
  :device-core:testDebugUnitTest \
  :device-driver-scale:testDebugUnitTest \
  :device-driver-cabinet:testDebugUnitTest \
  :device-starter-scale:testDebugUnitTest \
  :device-starter-cabinet:testDebugUnitTest \
  :device-api-base:publishReleasePublicationToMavenLocal \
  :device-api-scale:publishReleasePublicationToMavenLocal \
  :device-api-cabinet:publishReleasePublicationToMavenLocal \
  :device-core:publishReleasePublicationToMavenLocal \
  :device-driver-scale:publishReleasePublicationToMavenLocal \
  :device-driver-cabinet:publishReleasePublicationToMavenLocal \
  :device-starter-scale:publishReleasePublicationToMavenLocal \
  :device-starter-cabinet:publishReleasePublicationToMavenLocal
```

业务方接入 `foundation-core`：

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.lee-sq.AndroidPlatformWorkspace:foundation-core:foundation-core-v0.1.0")
}
```

业务方接入 `device`：

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // 称重设备
    implementation("com.github.lee-sq.AndroidPlatformWorkspace:device-starter-scale:device-v0.1.0")

    // 柜体设备
    implementation("com.github.lee-sq.AndroidPlatformWorkspace:device-starter-cabinet:device-v0.1.0")
}
```

新增 module 后，需要：

1. 在 `settings.gradle.kts` 添加 `include(":module-name")`。
2. 在 `jitpack.yml` 添加对应 tag 前缀和发布任务。
3. 使用 `<module-name>-v<semver>` 打 tag。

## 能力范围

本仓库适合沉淀：

- Android 平台基础能力
- 网络基础设施
- 通用应用、页面基类与轻量控件
- 跨 App 复用的工具类
- 可独立测试、独立发布的 library module

# Device Modules

`device` 包按框架层级组织目录，Gradle module 按稳定发布边界拆分。新增设备型号时，默认只在对应领域 driver module 内新增包目录，不新增 Gradle module。

## Module Layout

```text
device/
├── api/
│   ├── base/          # :device-api-base
│   ├── scale/         # :device-api-scale
│   └── cabinet/       # :device-api-cabinet
├── core/              # :device-core
├── driver/
│   ├── scale/         # :device-driver-scale
│   │   └── .../a,b    # 型号/厂商包目录
│   └── cabinet/       # :device-driver-cabinet
│       └── .../x,y    # 型号/厂商包目录
└── starter/
    ├── scale/         # :device-starter-scale
    └── cabinet/       # :device-starter-cabinet
```

## Driver Splitting Rule

- 默认：一个设备领域一个 driver module，例如 `:device-driver-scale`。
- 新增型号：在领域 driver module 下新增型号包，例如 `com.holderzone.device.driver.scale.c`。
- 例外：只有当厂商 AAR/native so 冲突、需要按型号独立发布、或业务必须按型号裁剪 APK 时，才把某个型号拆成独立 driver module。
- Starter 只聚合领域 driver module，并提供显式初始化入口。

## Cabinet Door Addressing

柜门能力使用顺序下标描述门位，而不是全局 `doorId`。

- `DoorAddress.primary(index)`：只寻址大柜门，`index` 是从 0 开始的顺序下标。
- `DoorAddress.compartment(primaryIndex, compartmentIndex)`：寻址某个大柜门下的小柜格，两个参数都是从 0 开始的顺序下标。
- UI 展示编号可以是 1-based，但进入 device 层前应转换为 0-based 下标。
- Driver 负责把 `DoorAddress` 转换成具体厂商协议里的门号、板号、锁号或命令字。

## Runtime Pipeline

`device-core` 已提供不依赖具体型号的运行期主链路：

1. `SerialPortManager.listPorts()` 通过 `SerialBackend` 枚举端口。
2. `AutoSniffer` 遍历端口、已注册 driver、driver 支持的 `SerialConfig`。
3. `ProbePipeline` 打开通道、发送探测帧、读取响应并交给 `IProbeStrategy` 判断。
4. 匹配成功后 `DeviceManager.bindSession()` 调用 `IDeviceFactory` 创建设备实例并建立完整运行会话。
5. `SerialReadLoop` 后台持续读取字节流。
6. `FrameStreamReader` 使用 driver 的 `IFrameParser` 处理半包、粘包并切出完整帧。
7. 对问答轮询型设备，driver 通过 `IPollingProvider` 声明命令，`SerialPollingLoop` 在 core 中统一调度写入。
8. `DeviceRuntime` 对主动上报和轮询设备启动看门狗，超时、读循环异常或轮询写入异常后优先尝试原端口重连，失败再全量探测。

## Real Serial Backend

`device-core` 已内置基于 `serialport-1.0.1` native so 的默认串口后端：

- `AndroidSerialBackend`：扫描 `/dev/ttyS*`、`/dev/ttyUSB*`、`/dev/ttyACM*` 路径，并创建真实串口通道。
- `AndroidSerialChannel`：通过 native `libserial_port.so` 打开/关闭串口，通过 `InputStream`/`OutputStream` 完成字节读写。
- `android_serialport_api.SerialPort`：为了匹配旧 so 写死的 JNI 符号而保留的最小 Java 壳，只承接 native `open/close`。

Demo AAR 里的 `SerialHelper` 读线程、发送线程、回调模型没有搬进来。半包、粘包、持续读循环仍然由本框架的 `SerialReadLoop`、`FrameStreamReader` 和各 driver 的 `IFrameParser` 处理。

默认使用方式：

```kotlin
val deviceManager = DeviceManager()
ScaleStarterInitializer.init(deviceManager)
deviceManager.startAutoSniffing()
```

如果后续需要替换为另一套 JNI、USB-OTG serial 库或厂商连接对象，仍然可以通过 `SerialBackend` 注入：

```kotlin
class CustomSerialBackend : SerialBackend {
    override fun listPorts(): List<SerialPortInfo> = ...

    override fun createChannel(portPath: String, config: SerialConfig): SerialChannel {
        return CustomSerialChannel(portPath, config)
    }
}

val deviceManager = DeviceManager(
    serialPortManager = SerialPortManager(CustomSerialBackend()),
)
```

当前内置 native 后端只保证 `8N1` 串口配置，也就是 `dataBits = 8`、`stopBits = 1`、`parity = NONE`。如果某个设备需要 `7E1`、`8E1` 等配置，应替换为支持完整 termios 参数的 `SerialBackend`。

厂商 AAR 不放进 `device-core`。如果某型号必须依赖厂商 AAR，把依赖放在对应领域 driver module 中，并且只用 `implementation`：

```kotlin
dependencies {
    implementation("com.vendor:vendor-cabinet-sdk:1.0.0")
}
```

Driver 公开签名仍然只能暴露 `device-api-*`、Kotlin/Java、Android SDK 类型。

## Real Driver Onboarding

新增真实硬件策略时，只需要在对应领域 driver module 新增型号包并实现 `IDeviceDriver`：

```kotlin
class VendorCabinetDriver : IDeviceDriver {
    override val descriptor = DriverDescriptor(
        strategyId = "cabinet.vendor.model",
        vendorName = "Vendor",
        deviceModel = "Model",
        deviceCategory = DeviceCategory.CABINET,
        communicationMode = CommunicationMode.ACTIVE_REPORT,
        supportedConfigs = listOf(SerialConfig(baudRate = 9_600)),
        capabilities = setOf(ILockable::class, IWeighable::class),
    )

    override val probeStrategy = VendorProbeStrategy()
    override val heartbeatProvider = VendorHeartbeatProvider()
    override val frameParser = VendorFrameParser()
    override val deviceFactory = VendorDeviceFactory()
}
```

然后在对应 starter 中注册：

```kotlin
deviceManager.registerDriver(VendorCabinetDriver())
```

称重能力同时提供单次读取和流式读取：

- `weigh()`：业务动作前主动读取一次。
- `observeWeight()`：业务 UI 实时展示时 collect，主动上报型设备直接转发帧，被动响应型设备由 driver 内部轮询。

## ProGuard / R8

所有 `device` Android library 的 `release` 产物默认开启 R8 混淆，并通过 `consumer-rules.pro` 向业务 App 自动传递必要规则。业务方正常只需要依赖对应 AAR/Maven 坐标，不需要重复维护 device 的 keep 规则。

- `device-api-*` 保留对外 API 的类名和 public/protected 成员，保证业务代码、能力接口和数据模型的调用边界稳定。
- `device-core` 保留运行时入口和 `android_serialport_api.SerialPort`。其中 `SerialPort` 必须完整 keep，因为 `libserial_port.so` 写死了 JNI 符号。
- `device-driver-*` 保留可手动注册的 `*Driver` 入口、公开成员，以及公开签名里出现的类型；其余内部协议解析、探测、设备实现仍允许 R8 混淆。
- `device-starter-*` 保留显式初始化入口，推荐业务方通过 Starter 注册对应领域驱动。

后续如果某个 driver 引入厂商 AAR、反射创建、JSON 序列化、ServiceLoader 或新的 JNI/native so，应把对应 keep / dontwarn 规则放进该 driver module 的 `consumer-rules.pro`，让业务方自动继承。

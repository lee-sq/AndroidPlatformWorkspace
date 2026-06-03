package com.holderzone.device.testapp

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.holderzone.device.api.base.logging.DeviceLogEntry
import com.holderzone.device.api.base.logging.DeviceLogLevel
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.SerialPortManager
import com.holderzone.device.core.device.DeviceConnectionEvent
import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.scale.jw.JWScaleDriver
import com.holderzone.device.starter.scale.ScaleStarterInitializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val deviceManager = DeviceManager()
    private val serialPortManager = SerialPortManager()
    private val driver = JWScaleDriver()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA)
    private val defaultPortPath = JWScaleDriver.DEFAULT_PORT_PATH.firstOrNull() ?: FALLBACK_PORT_PATH

    private lateinit var statusText: TextView
    private lateinit var portText: TextView
    private lateinit var deviceText: TextView
    private lateinit var weightText: TextView
    private lateinit var detailText: TextView
    private lateinit var logText: TextView
    private lateinit var portSpinner: Spinner
    private lateinit var portAdapter: ArrayAdapter<String>

    private var selectedPort: String = defaultPortPath
    private var weightJob: Job? = null
    private var currentDeviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScaleStarterInitializer.init(deviceManager)
        setContentView(buildContentView())
        observeDeviceLogs()
        observeGlobalState()
        observeDevices()
        refreshPorts()
        appendLog("测试程序已启动，默认串口：$defaultPortPath")
    }

    override fun onDestroy() {
        deviceManager.clear()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFFF8FAFC.toInt())
        }

        val title = TextView(this).apply {
            text = "电子秤设备测试"
            textSize = 24f
            setTextColor(0xFF0F172A.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(title, matchWrap())

        weightText = TextView(this).apply {
            text = "--.- 千克"
            textSize = 48f
            setTextColor(0xFF0F766E.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 20)
        }
        root.addView(weightText, matchWrap())

        statusText = infoText("全局状态：未连接")
        portText = infoText("串口：-")
        deviceText = infoText("设备：-")
        detailText = infoText("详情：等待连接")
        root.addView(statusText, matchWrap())
        root.addView(portText, matchWrap())
        root.addView(deviceText, matchWrap())
        root.addView(detailText, matchWrap())

        portAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mutableListOf(selectedPort)
        )
        portSpinner = Spinner(this).apply {
            adapter = portAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedPort = portAdapter.getItem(position) ?: defaultPortPath
                    portText.text = "串口：$selectedPort"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(portSpinner, matchWrap())

        root.addView(
            buttonRow(
                button("刷新串口") { refreshPorts() },
                button("自动嗅探") { startAutoSniff() },
            )
        )
        root.addView(
            buttonRow(
                button("绑定所选串口") { bindSelectedPort() },
                button("绑定 $defaultPortPath") {
                    selectedPort = defaultPortPath
                    bindPort(defaultPortPath)
                },
            )
        )
        root.addView(
            buttonRow(
                button("去皮") { callScale("去皮") { it.tare() } },
                button("清零") { callScale("清零") { it.zero() } },
            )
        )

        logText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF334155.toInt())
            setTextIsSelectable(true)
            text = ""
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(16, 16, 16, 16)
            addView(logText)
        }
        root.addView(
            scroll, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                topMargin = 20
            })

        return root
    }

    private fun observeGlobalState() {
        scope.launch {
            deviceManager.globalState.collectLatest { state ->
                statusText.text = "全局状态：${state.toChineseText()}"
                appendLog("全局状态变更：${state.toChineseText()}")
            }
        }
    }

    private fun observeDeviceLogs() {
        deviceManager.minLogLevel = DeviceLogLevel.INFO
        scope.launch {
            deviceManager.logs.collectLatest { entry ->
                appendLog(entry.toDisplayText())
            }
        }
    }

    private fun refreshPorts() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { serialPortManager.listPorts().map { it.path } }
            }.onSuccess { ports ->
                val items: List<String> = (listOf(defaultPortPath) + ports).distinct()
                portAdapter.clear()
                portAdapter.addAll(items)
                portAdapter.notifyDataSetChanged()
                val selectedIndex = items.indexOf(selectedPort).takeIf { it >= 0 } ?: 0
                portSpinner.setSelection(selectedIndex)
                appendLog("串口列表：${items.joinToString()}")
            }.onFailure { error ->
                appendLog("刷新串口失败：${error.message}")
            }
        }
    }

    private fun startAutoSniff() {
        appendLog("开始自动嗅探")
        clearCurrentSubscription()
        deviceManager.startAutoSniffing()
    }

    private fun bindSelectedPort() {
        bindPort(selectedPort)
    }

    private fun bindPort(portPath: String) {
        appendLog("绑定串口：$portPath，波特率：${JWScaleDriver.DEFAULT_BAUD_RATE}")
        clearCurrentSubscription()
        scope.launch {
            runCatching {
                val channel = serialPortManager.openChannel(
                    portPath = portPath,
                    config = SerialConfig(baudRate = JWScaleDriver.DEFAULT_BAUD_RATE),
                )
                withContext(Dispatchers.IO) {
                    deviceManager.bindDeviceSession(driver, channel)
                }
            }.onSuccess { device ->
                appendLog("绑定成功：${device.info.deviceId}")
            }.onFailure { error ->
                appendLog("绑定失败：${error.message}")
                detailText.text = "详情：绑定失败"
            }
        }
    }

    private fun subscribeScale(deviceId: String) {
        if (currentDeviceId == deviceId) return
        currentDeviceId = deviceId
        deviceText.text = "设备：$deviceId"
        portText.text =
            "串口：${deviceId.substringAfter(':', missingDelimiterValue = selectedPort)}"
        weightJob?.cancel()
        weightJob = scope.launch {
            val weighable = deviceManager.queryCapability<IWeighable>(deviceId)
            if (weighable == null) {
                appendLog("设备不支持称重能力：$deviceId")
                return@launch
            }
            appendLog("已订阅重量数据流")
            weighable.observeWeight().collectLatest { result ->
                showWeight(result)
            }
        }
    }

    private fun observeDevices() {
        scope.launch {
            deviceManager.devicesFlow
                .map { devices -> devices.firstOrNull()?.info?.deviceId }
                .distinctUntilChanged()
                .collectLatest { deviceId ->
                    val snapshotText = deviceId ?: "-"
                    deviceText.text = "设备：$snapshotText"
                }
        }
        scope.launch {
            deviceManager.connectionEvents.collectLatest { event ->
                when (event) {
                    is DeviceConnectionEvent.Bound -> {
                        val deviceId = event.record.device.info.deviceId
                        appendLog("设备已绑定：$deviceId")
                        subscribeScale(deviceId)
                    }

                    is DeviceConnectionEvent.StateChanged -> {
                        appendLog("设备状态变更：${event.deviceId}，${event.state.toChineseText()}")
                    }

                    is DeviceConnectionEvent.Unbound -> {
                        appendLog("设备已解绑：${event.deviceId}")
                        if (currentDeviceId == event.deviceId) {
                            clearCurrentSubscription()
                        }
                    }

                    DeviceConnectionEvent.Cleared -> {
                        appendLog("设备管理器已清空")
                        clearCurrentSubscription()
                    }
                }
            }
        }
    }

    private fun showWeight(result: WeightResult) {
        weightText.text = "%.1f %s".format(result.value, result.unit.toChineseText())
        detailText.text =
            "详情：${if (result.stable) "稳定" else "波动"}，基础值：${result.grams} 克，时间戳：${result.timestampMs}"
//        appendLog("重量：${result.value} ${result.unit.toChineseText()}，基础值：${result.grams} 克，状态：${if (result.stable) "稳定" else "波动"}")
    }

    private fun callScale(actionName: String, block: suspend (IWeighable) -> Unit) {
        val deviceId = currentDeviceId
        if (deviceId == null) {
            appendLog("$actionName 跳过：尚未绑定设备")
            return
        }
        scope.launch {
            runCatching {
                val weighable = deviceManager.queryCapability<IWeighable>(deviceId)
                    ?: error("设备不支持称重能力")
                withContext(Dispatchers.IO) {
                    block(weighable)
                }
            }.onSuccess {
                appendLog("$actionName 指令已发送")
            }.onFailure { error ->
                appendLog("$actionName 失败：${error.message}")
            }
        }
    }

    private fun clearCurrentSubscription() {
        weightJob?.cancel()
        weightJob = null
        currentDeviceId = null
        deviceText.text = "设备：-"
        detailText.text = "详情：等待连接"
    }

    private fun appendLog(message: String) {
        val line = "${timeFormat.format(Date())}  $message"
        logText.text = (line + "\n" + logText.text).take(MAX_LOG_CHARS)
    }

    private fun infoText(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(0xFF1E293B.toInt())
            setPadding(0, 5, 0, 5)
        }
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }
    }

    private fun buttonRow(left: Button, right: Button): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, rowButtonParams())
            addView(right, rowButtonParams())
        }
    }

    private fun rowButtonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(4, 8, 4, 0)
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun ConnectionState.toChineseText(): String {
        return when (this) {
            ConnectionState.DISCONNECTED -> "未连接"
            ConnectionState.SNIFFING -> "嗅探中"
            ConnectionState.CONNECTING -> "连接中"
            ConnectionState.CONNECTED -> "已连接"
            ConnectionState.DEGRADED -> "异常降级"
            ConnectionState.RECONNECTING -> "重连中"
            ConnectionState.FAILED -> "连接失败"
        }
    }

    private fun WeightUnit.toChineseText(): String {
        return when (this) {
            WeightUnit.GRAM -> "克"
            WeightUnit.KILOGRAM -> "千克"
            WeightUnit.POUND -> "磅"
            WeightUnit.OUNCE -> "盎司"
        }
    }

    private fun DeviceLogEntry.toDisplayText(): String {
        val details = listOfNotNull(
            deviceId?.let { "设备=$it" },
            strategyId?.let { "策略=$it" },
            portPath?.let { "端口=$it" },
            throwable?.message?.let { "异常=$it" },
        ).joinToString("，")
        val suffix = if (details.isBlank()) "" else "（$details）"
        return "设备日志[${level.toChineseText()}][$tag] $message$suffix"
    }

    private fun DeviceLogLevel.toChineseText(): String {
        return when (this) {
            DeviceLogLevel.DEBUG -> "调试"
            DeviceLogLevel.INFO -> "信息"
            DeviceLogLevel.WARN -> "警告"
            DeviceLogLevel.ERROR -> "错误"
        }
    }

    private companion object {
        const val MAX_LOG_CHARS = 12_000
        const val FALLBACK_PORT_PATH = "/dev/ttyS7"
    }
}

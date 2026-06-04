package com.holderzone.device.cabinettestapp

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.holderzone.device.api.base.logging.DeviceLogEntry
import com.holderzone.device.api.base.logging.DeviceLogLevel
import com.holderzone.device.api.base.model.ConnectionState
import com.holderzone.device.api.base.model.SerialConfig
import com.holderzone.device.api.cabinet.capability.ILockable
import com.holderzone.device.api.cabinet.capability.IPrintable
import com.holderzone.device.api.cabinet.capability.ITemperatureCtrl
import com.holderzone.device.api.cabinet.model.DoorAddress
import com.holderzone.device.api.cabinet.model.DoorState
import com.holderzone.device.api.cabinet.model.PrintContent
import com.holderzone.device.api.cabinet.model.PrintResult
import com.holderzone.device.api.cabinet.model.PrinterStatus
import com.holderzone.device.api.cabinet.model.TemperatureReading
import com.holderzone.device.api.scale.capability.ICalibratable
import com.holderzone.device.api.scale.capability.IWeighable
import com.holderzone.device.api.scale.model.WeightResult
import com.holderzone.device.api.scale.model.WeightUnit
import com.holderzone.device.core.channel.SelfManagedSerialChannel
import com.holderzone.device.core.channel.SerialPortManager
import com.holderzone.device.core.device.DeviceConnectionEvent
import com.holderzone.device.core.device.DeviceManager
import com.holderzone.device.driver.cabinet.jw.serial.JwSerialCabinetDriver
import com.holderzone.device.driver.cabinet.jw.serial.protocol.JwCabinetProtocol
import com.holderzone.device.driver.cabinet.star.StarCabinetDriver
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
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA)

    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var portText: TextView
    private lateinit var strategyText: TextView
    private lateinit var weightText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var doorStateText: TextView
    private lateinit var capabilityText: TextView
    private lateinit var logText: TextView
    private lateinit var portSpinner: Spinner
    private lateinit var doorSpinner: Spinner
    private lateinit var calibrateInput: EditText
    private lateinit var temperatureInput: EditText
    private lateinit var portAdapter: ArrayAdapter<String>
    private lateinit var doorAdapter: ArrayAdapter<String>

    private var selectedPort: String = DEFAULT_JW_CABINET_PORT
    private var selectedDoorNo: Int = 1
    private var currentDeviceId: String? = null
    private var currentStrategyId: String? = null
    private var currentPortPath: String? = null
    private var weightJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceManager.minLogLevel = DeviceLogLevel.INFO

        setContentView(buildContentView())
        resetRegisteredDrivers(DriverSet.ALL)
        observeDeviceLogs()
        observeGlobalState()
        observeDevices()
        refreshPorts()
        appendLog("测试程序已启动，已注册 JW 留样柜和 Star 留样柜驱动")
    }

    override fun onDestroy() {
        clearWeightSubscription()
        deviceManager.clear()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildContentView(): View {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val root = LinearLayout(this).apply {
            orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xFFF8FAFC.toInt())
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 6, 6, 6)
        }

        val title = TextView(this).apply {
            text = "留样柜实体机测试"
            textSize = if (isLandscape) 21f else 24f
            setTextColor(0xFF0F172A.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        controls.addView(title, matchWrap())

        weightText = TextView(this).apply {
            text = "--.- 克"
            textSize = if (isLandscape) 34f else 42f
            setTextColor(0xFF0F766E.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 8)
        }

        temperatureText = infoText("温度：-")
        doorStateText = infoText("门状态：-")
        statusText = infoText("全局状态：未连接")
        deviceText = infoText("设备：-")
        strategyText = infoText("策略：-")
        portText = infoText("串口：-")
        capabilityText = infoText("能力：-")

        portAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            mutableListOf(selectedPort),
        )
        portSpinner = Spinner(this).apply {
            adapter = portAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selectedPort = portAdapter.getItem(position) ?: DEFAULT_JW_CABINET_PORT
                    if (currentPortPath == null) {
                        portText.text = "串口：$selectedPort"
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        controls.addView(statusText, matchWrap())
        controls.addView(portText, matchWrap())
        controls.addView(labelText("串口"), matchWrap())
        controls.addView(portSpinner, matchWrap())
        controls.addView(
            buttonRow(
                button("刷新串口") { refreshPorts() },
                button("自动嗅探") { startAutoSniff() },
            ),
        )
        controls.addView(
            buttonRow(
                button("只嗅探 JW") { startAutoSniff(DriverSet.JW_ONLY) },
                button("只嗅探 Star") { startAutoSniff(DriverSet.STAR_ONLY) },
            ),
        )
        controls.addView(
            buttonRow(
                button("绑定 JW") { bindJwSelectedPort() },
                button("绑定 Star") { bindStarSelectedPort() },
            ),
        )
        controls.addView(deviceText, matchWrap())
        controls.addView(strategyText, matchWrap())
        controls.addView(capabilityText, matchWrap())
        controls.addView(weightText, matchWrap())
        controls.addView(temperatureText, matchWrap())
        controls.addView(doorStateText, matchWrap())

        doorAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (1..DEFAULT_DOOR_COUNT).map { "第 $it 门" },
        )
        doorSpinner = Spinner(this).apply {
            adapter = doorAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selectedDoorNo = position + 1
                    doorStateText.text = "门状态：第 $selectedDoorNo 门，待查询"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        controls.addView(labelText("测试门号"), matchWrap())
        controls.addView(doorSpinner, matchWrap())

        calibrateInput = editText("1000")
        temperatureInput = editText("4")
        controls.addView(inputRow("标定克数", calibrateInput, "目标温度", temperatureInput))

        controls.addView(
            buttonRow(
                button("开门") { unlockSelectedDoor() },
                button("关门/上锁") { lockSelectedDoor() },
            ),
        )
        controls.addView(
            buttonRow(
                button("查询门状态") { querySelectedDoorState() },
                button("读取温度") { readTemperature() },
            ),
        )
        controls.addView(
            buttonRow(
                button("设置温度") { setTemperature() },
                button("读取重量") { readWeightOnce() },
            ),
        )
        controls.addView(
            buttonRow(
                button("订阅重量") { subscribeCurrentWeight(force = true) },
                button("停止订阅") { stopWeightSubscription() },
            ),
        )
        controls.addView(
            buttonRow(
                button("去皮") { callWeighable("去皮") { it.tare() } },
                button("置零") { callWeighable("置零") { it.zero() } },
            ),
        )
        controls.addView(
            buttonRow(
                button("标定") { calibrateScale() },
                button("打印测试") { printTestLabel() },
            ),
        )
        controls.addView(
            buttonRow(
                button("打印机状态") { queryPrinterStatus() },
                button("清空设备") { clearDevices() },
            ),
        )
        controls.addView(
            buttonRow(
                button("清空日志") { clearLogs() },
                button("停止嗅探") { stopAutoSniff() },
            ),
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
        val controlsScroll = ScrollView(this).apply {
            isFillViewport = false
            addView(controls)
        }
        val logPane = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(labelText("日志"), matchWrap())
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
        }
        if (isLandscape) {
            root.addView(
                controlsScroll,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.05f,
                ).apply {
                    rightMargin = 12
                },
            )
            root.addView(
                logPane,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0.95f,
                ),
            )
        } else {
            root.addView(
                controlsScroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1.7f,
                ),
            )
            root.addView(
                logPane,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ).apply {
                    topMargin = 12
                },
            )
        }

        return root
    }

    private fun observeDeviceLogs() {
        scope.launch {
            deviceManager.logs.collectLatest { entry ->
                appendLog(entry.toDisplayText())
            }
        }
    }

    private fun observeGlobalState() {
        scope.launch {
            deviceManager.globalState.collectLatest { state ->
                statusText.text = "全局状态：${state.toChineseText()}"
                appendLog("全局状态变更：${state.toChineseText()}")
            }
        }
    }

    private fun observeDevices() {
        scope.launch {
            deviceManager.deviceRecords
                .map { records -> records.firstOrNull() }
                .distinctUntilChanged()
                .collectLatest { record ->
                    if (record == null) {
                        currentDeviceId = null
                        currentStrategyId = null
                        currentPortPath = null
                        renderCurrentDevice()
                        renderCapabilities(emptySet())
                    } else {
                        currentDeviceId = record.device.info.deviceId
                        currentStrategyId = record.driver.descriptor.strategyId
                        currentPortPath = record.device.info.deviceId.substringAfter(
                            delimiter = ":",
                            missingDelimiterValue = selectedPort,
                        )
                        renderCurrentDevice()
                        renderCapabilities(record.capabilities.keys)
                        subscribeCurrentWeight(force = false)
                    }
                }
        }
        scope.launch {
            deviceManager.connectionEvents.collectLatest { event ->
                when (event) {
                    is DeviceConnectionEvent.Bound -> {
                        appendLog("设备已绑定：${event.record.device.info.deviceId}")
                    }

                    is DeviceConnectionEvent.StateChanged -> {
                        appendLog(
                            "设备状态变更：${event.deviceId}，" +
                                "${event.previousState?.toChineseText() ?: "-"} -> ${event.state.toChineseText()}",
                        )
                    }

                    is DeviceConnectionEvent.Unbound -> {
                        appendLog("设备已解绑：${event.deviceId}")
                        if (currentDeviceId == event.deviceId) {
                            clearWeightSubscription()
                        }
                    }

                    DeviceConnectionEvent.Cleared -> {
                        appendLog("设备管理器已清空")
                        clearWeightSubscription()
                    }
                }
            }
        }
    }

    private fun refreshPorts() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { serialPortManager.listPorts().map { it.path } }
            }.onSuccess { ports ->
                val items = (
                    listOf(
                        DEFAULT_JW_CABINET_PORT,
                        DEFAULT_STAR_CABINET_PORT,
                        DEFAULT_JW_PRINTER_PORT,
                        DEFAULT_STAR_PRINTER_PORT,
                    ) + ports
                    ).distinct()
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

    private fun startAutoSniff(driverSet: DriverSet = DriverSet.ALL) {
        resetRegisteredDrivers(driverSet)
        appendLog("开始自动嗅探留样柜")
        clearWeightSubscription()
        deviceManager.startAutoSniffing()
    }

    private fun stopAutoSniff() {
        deviceManager.stopAutoSniffing()
        appendLog("已请求停止自动嗅探")
    }

    private fun bindJwSelectedPort() {
        appendLog("手动绑定 JW 留样柜：$selectedPort，波特率：${JwCabinetProtocol.DEFAULT_BAUD_RATE}")
        clearWeightSubscription()
        scope.launch {
            runCatching {
                val channel = serialPortManager.openChannel(
                    portPath = selectedPort,
                    config = SerialConfig(baudRate = JwCabinetProtocol.DEFAULT_BAUD_RATE),
                )
                withContext(Dispatchers.IO) {
                    deviceManager.bindSession(JwSerialCabinetDriver(applicationContext), channel)
                }
            }.onSuccess { device ->
                appendLog("JW 绑定成功：${device.info.deviceId}")
            }.onFailure { error ->
                appendLog("JW 绑定失败：${error.message}")
            }
        }
    }

    private fun bindStarSelectedPort() {
        appendLog("手动绑定 Star 留样柜：$selectedPort，波特率：${StarCabinetDriver.DEFAULT_CABINET_BAUD_RATE}")
        clearWeightSubscription()
        scope.launch {
            runCatching {
                val driver = StarCabinetDriver(
                    context = applicationContext,
                    cabinetPortPath = selectedPort,
                )
                val channel = SelfManagedSerialChannel(
                    portPath = selectedPort,
                    config = SerialConfig(baudRate = StarCabinetDriver.DEFAULT_CABINET_BAUD_RATE),
                )
                withContext(Dispatchers.IO) {
                    deviceManager.bindSession(driver, channel)
                }
            }.onSuccess { device ->
                appendLog("Star 绑定成功：${device.info.deviceId}")
            }.onFailure { error ->
                appendLog("Star 绑定失败：${error.message}")
            }
        }
    }

    private fun unlockSelectedDoor() {
        val address = selectedDoorAddress()
        callLockable("打开第 $selectedDoorNo 门") { lockable ->
            val success = lockable.unlock(address)
            appendLog("开门结果：${success.toChineseText()}")
        }
    }

    private fun lockSelectedDoor() {
        val address = selectedDoorAddress()
        callLockable("关闭/上锁第 $selectedDoorNo 门") { lockable ->
            val success = lockable.lock(address)
            appendLog("关门/上锁结果：${success.toChineseText()}（部分驱动暂不支持主动上锁）")
        }
    }

    private fun querySelectedDoorState() {
        val address = selectedDoorAddress()
        callLockable("查询第 $selectedDoorNo 门状态") { lockable ->
            val state = lockable.queryDoorState(address)
            doorStateText.text = "门状态：第 $selectedDoorNo 门，${state.toChineseText()}"
            appendLog("第 $selectedDoorNo 门状态：${state.toChineseText()}")
        }
    }

    private fun readWeightOnce() {
        callWeighable("读取重量") { weighable ->
            val result = weighable.weigh()
            showWeight(result)
            appendLog("当前重量：${result.toDisplayText()}")
        }
    }

    private fun subscribeCurrentWeight(force: Boolean) {
        val deviceId = currentDeviceId
        if (deviceId == null) {
            if (force) appendLog("订阅重量跳过：尚未绑定设备")
            return
        }
        if (!force && weightJob != null) return
        clearWeightSubscription()
        weightJob = scope.launch {
            val weighable = deviceManager.queryCapability<IWeighable>(deviceId)
            if (weighable == null) {
                if (force) appendLog("订阅重量失败：当前设备不支持称重能力")
                return@launch
            }
            appendLog("已订阅重量数据流")
            weighable.observeWeight().collectLatest { result ->
                showWeight(result)
            }
        }
    }

    private fun stopWeightSubscription() {
        clearWeightSubscription()
        appendLog("已停止订阅重量")
    }

    private fun calibrateScale() {
        val grams = calibrateInput.text?.toString()?.trim()?.toDoubleOrNull()
        if (grams == null || grams <= 0.0) {
            appendLog("标定失败：请输入大于 0 的标准克数")
            return
        }
        callCapability<ICalibratable>("按 ${grams.formatNumber()} 克标定") { calibratable ->
            val result = calibratable.calibrate(grams)
            appendLog(
                "标定结果：${result.success.toChineseText()}，" +
                    "系数：${result.slope?.formatNumber() ?: "-"}，" +
                    "原始重量：${result.rawWeightGrams?.formatNumber() ?: "-"} 克，" +
                    "零点偏移：${result.zeroOffsetGrams?.formatNumber() ?: "-"} 克",
            )
        }
    }

    private fun readTemperature() {
        callCapability<ITemperatureCtrl>("读取温度") { temperatureCtrl ->
            val reading = temperatureCtrl.getTemperature()
            showTemperature(reading)
            appendLog("当前温度：${reading.toDisplayText()}")
        }
    }

    private fun setTemperature() {
        val celsius = temperatureInput.text?.toString()?.trim()?.toDoubleOrNull()
        if (celsius == null) {
            appendLog("设置温度失败：请输入目标温度")
            return
        }
        callCapability<ITemperatureCtrl>("设置温度 ${celsius.formatNumber()}℃") { temperatureCtrl ->
            val success = temperatureCtrl.setTemperature(celsius)
            appendLog("设置温度结果：${success.toChineseText()}")
        }
    }

    private fun printTestLabel() {
        val deviceId = currentDeviceId
        val content = PrintContent(
            title = "留样柜测试",
            lines = listOf(
                "时间：${timeFormat.format(Date())}",
                "设备：${deviceId ?: "-"}",
                "串口：${currentPortPath ?: selectedPort}",
                "门号：第 $selectedDoorNo 门",
            ),
        )
        callCapability<IPrintable>("打印测试标签") { printable ->
            val result = printable.print(content)
            appendLog("打印结果：${result.toDisplayText()}")
        }
    }

    private fun queryPrinterStatus() {
        callCapability<IPrintable>("查询打印机状态") { printable ->
            val status = printable.getPrinterStatus()
            appendLog("打印机状态：${status.toChineseText()}")
        }
    }

    private fun clearDevices() {
        clearWeightSubscription()
        deviceManager.clear()
        resetRegisteredDrivers(DriverSet.ALL)
        appendLog("已清空设备，并重新注册留样柜驱动")
    }

    private fun resetRegisteredDrivers(driverSet: DriverSet) {
        deviceManager.unregisterDriver(JwSerialCabinetDriver.STRATEGY_ID)
        deviceManager.unregisterDriver(StarCabinetDriver.STRATEGY_ID)
        when (driverSet) {
            DriverSet.ALL -> {
                deviceManager.registerDriver(StarCabinetDriver(applicationContext))
                deviceManager.registerDriver(JwSerialCabinetDriver(applicationContext))
                appendLog("已注册 JW + Star 留样柜驱动")
            }
            DriverSet.JW_ONLY -> {
                deviceManager.registerDriver(JwSerialCabinetDriver(applicationContext))
                appendLog("已切换为只嗅探 JW 留样柜")
            }
            DriverSet.STAR_ONLY -> {
                deviceManager.registerDriver(StarCabinetDriver(applicationContext))
                appendLog("已切换为只嗅探 Star 留样柜")
            }
        }
    }

    private fun clearLogs() {
        logText.text = ""
        appendLog("日志已清空")
    }

    private fun callWeighable(actionName: String, block: suspend (IWeighable) -> Unit) {
        callCapability(actionName, block)
    }

    private fun callLockable(actionName: String, block: suspend (ILockable) -> Unit) {
        callCapability(actionName, block)
    }

    private inline fun <reified T : Any> callCapability(
        actionName: String,
        crossinline block: suspend (T) -> Unit,
    ) {
        val deviceId = currentDeviceId
        if (deviceId == null) {
            appendLog("$actionName 跳过：尚未绑定设备")
            return
        }
        scope.launch {
            runCatching {
                val capability = deviceManager.queryCapability<T>(deviceId)
                    ?: error("当前设备不支持 ${T::class.java.simpleName}")
                withContext(Dispatchers.IO) {
                    block(capability)
                }
            }.onSuccess {
                appendLog("$actionName 指令执行完成")
            }.onFailure { error ->
                appendLog("$actionName 失败：${error.message}")
            }
        }
    }

    private fun selectedDoorAddress(): DoorAddress {
        return DoorAddress.primary(selectedDoorNo - 1)
    }

    private fun renderCurrentDevice() {
        deviceText.text = "设备：${currentDeviceId ?: "-"}"
        strategyText.text = "策略：${currentStrategyId ?: "-"}"
        portText.text = "串口：${currentPortPath ?: selectedPort}"
    }

    private fun renderCapabilities(capabilities: Set<Class<*>>) {
        if (capabilities.isEmpty()) {
            capabilityText.text = "能力：-"
            return
        }
        val labels = listOfNotNull(
            "称重".takeIf { IWeighable::class.java in capabilities },
            "标定".takeIf { ICalibratable::class.java in capabilities },
            "门锁".takeIf { ILockable::class.java in capabilities },
            "温控".takeIf { ITemperatureCtrl::class.java in capabilities },
            "打印".takeIf { IPrintable::class.java in capabilities },
        )
        capabilityText.text = "能力：${labels.joinToString(" / ")}"
    }

    private fun showWeight(result: WeightResult) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            weightText.text = result.toDisplayText()
        } else {
            weightText.post {
                weightText.text = result.toDisplayText()
            }
        }
    }

    private fun showTemperature(reading: TemperatureReading) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            temperatureText.text = "温度：${reading.toDisplayText()}"
        } else {
            temperatureText.post {
                temperatureText.text = "温度：${reading.toDisplayText()}"
            }
        }
    }

    private fun clearWeightSubscription() {
        weightJob?.cancel()
        weightJob = null
    }

    private fun appendLog(message: String) {
        val update = {
            val line = "${timeFormat.format(Date())}  $message"
            logText.text = (line + "\n" + logText.text).take(MAX_LOG_CHARS)
        }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            update()
        } else {
            logText.post(update)
        }
    }

    private fun infoText(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(0xFF1E293B.toInt())
            setPadding(0, 4, 0, 4)
        }
    }

    private fun labelText(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
            setPadding(0, 10, 0, 2)
        }
    }

    private fun editText(value: String): EditText {
        return EditText(this).apply {
            setText(value)
            textSize = 15f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setPadding(12, 8, 12, 8)
        }
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            minHeight = 48
            setOnClickListener { onClick() }
        }
    }

    private fun inputRow(
        leftLabel: String,
        leftInput: EditText,
        rightLabel: String,
        rightInput: EditText,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(inputGroup(leftLabel, leftInput), rowButtonParams())
            addView(inputGroup(rightLabel, rightInput), rowButtonParams())
        }
    }

    private fun inputGroup(label: String, input: EditText): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(labelText(label), matchWrap())
            addView(input, matchWrap())
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

    private fun WeightResult.toDisplayText(): String {
        return "${value.formatNumber()} ${unit.toChineseText()}"
    }

    private fun TemperatureReading.toDisplayText(): String {
        return if (celsius.isNaN()) {
            "未知"
        } else {
            "${celsius.formatNumber()}℃"
        }
    }

    private fun PrintResult.toDisplayText(): String {
        val messageText = message?.let { "，$it" }.orEmpty()
        return "${success.toChineseText()}$messageText"
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

    private fun DeviceLogLevel.toChineseText(): String {
        return when (this) {
            DeviceLogLevel.DEBUG -> "调试"
            DeviceLogLevel.INFO -> "信息"
            DeviceLogLevel.WARN -> "警告"
            DeviceLogLevel.ERROR -> "错误"
        }
    }

    private fun DoorState.toChineseText(): String {
        return when (this) {
            DoorState.OPEN -> "已打开"
            DoorState.CLOSED -> "已关闭"
            DoorState.LOCKED -> "已锁定"
            DoorState.UNKNOWN -> "未知"
        }
    }

    private fun PrinterStatus.toChineseText(): String {
        return when (this) {
            PrinterStatus.READY -> "就绪"
            PrinterStatus.BUSY -> "忙碌"
            PrinterStatus.PAPER_OUT -> "缺纸"
            PrinterStatus.OFFLINE -> "离线"
            PrinterStatus.UNKNOWN -> "未知"
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

    private fun Boolean.toChineseText(): String {
        return if (this) "成功" else "失败"
    }

    private fun Double.formatNumber(): String {
        return if (this % 1.0 == 0.0) {
            "%.0f".format(Locale.CHINA, this)
        } else {
            "%.2f".format(Locale.CHINA, this)
        }
    }

    private enum class DriverSet {
        ALL,
        JW_ONLY,
        STAR_ONLY,
    }

    private companion object {
        const val MAX_LOG_CHARS = 16_000
        const val DEFAULT_DOOR_COUNT = 8
        const val DEFAULT_JW_CABINET_PORT = JwCabinetProtocol.DEFAULT_CABINET_PORT
        const val DEFAULT_JW_PRINTER_PORT = JwCabinetProtocol.DEFAULT_PRINTER_PORT
        const val DEFAULT_STAR_CABINET_PORT = StarCabinetDriver.DEFAULT_CABINET_PORT
        const val DEFAULT_STAR_PRINTER_PORT = StarCabinetDriver.DEFAULT_PRINTER_PORT
    }
}
